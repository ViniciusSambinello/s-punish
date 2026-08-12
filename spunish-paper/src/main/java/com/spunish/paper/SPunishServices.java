package com.spunish.paper;

import com.spunish.common.catalog.CatalogLoadResult;
import com.spunish.common.catalog.CatalogValidationError;
import com.spunish.common.catalog.ReasonCatalogFile;
import com.spunish.common.catalog.ReasonCatalogHolder;
import com.spunish.common.catalog.ReasonCatalogValidator;
import com.spunish.common.config.ConfigLoadException;
import com.spunish.common.config.SpunishConfig;
import com.spunish.common.config.YamlConfigLoader;
import com.spunish.common.domain.DefaultSystemClock;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.gui.GuiConfig;
import com.spunish.common.message.MessageService;
import com.spunish.common.platform.AudienceResolver;
import com.spunish.common.platform.ConfiguredServerIdentity;
import com.spunish.common.platform.MainThreadDispatcher;
import com.spunish.common.platform.PermissionChecker;
import com.spunish.common.platform.PlayerKicker;
import com.spunish.common.platform.ServerIdentity;
import com.spunish.common.service.PunishmentHistoryService;
import com.spunish.common.service.PunishmentIssueService;
import com.spunish.common.service.PunishmentRevokeService;
import com.spunish.common.service.ReportService;
import com.spunish.common.storage.ConnectionHealthMonitor;
import com.spunish.common.storage.DatabaseConnectionProvider;
import com.spunish.common.storage.IoExecutor;
import com.spunish.common.storage.MySqlProfileRepository;
import com.spunish.common.storage.MySqlPunishmentRepository;
import com.spunish.common.storage.MySqlReportRepository;
import com.spunish.common.storage.MySqlSyncEventRepository;
import com.spunish.common.storage.ProfileRepository;
import com.spunish.common.storage.RetentionService;
import com.spunish.common.storage.SchemaMigrator;
import com.spunish.common.storage.TableNames;
import com.spunish.common.sync.PunishmentStateCache;
import com.spunish.common.sync.SyncEventConsumer;
import com.spunish.paper.enforcement.BanEnforcer;
import com.spunish.paper.enforcement.StaffNotifier;
import com.spunish.paper.platform.PaperAudienceResolver;
import com.spunish.paper.platform.PaperMainThreadDispatcher;
import com.spunish.paper.platform.PaperPermissionChecker;
import com.spunish.paper.platform.PaperPlayerKicker;
import com.spunish.paper.sync.PaperSyncEventListener;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.ConfigurationNode;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class SPunishServices implements AutoCloseable {

    private final Logger logger;
    private final SpunishConfig config;
    private final Path reasonsFile;
    private final Path messagesFile;
    private final ReasonCatalogHolder catalogHolder;
    private final MessageService messageService;
    private final GuiConfig guiConfig;

    private final DatabaseConnectionProvider connectionProvider;
    private final TableNames tableNames;
    private final IoExecutor ioExecutor;
    private final ProfileRepository profileRepository;
    private final MySqlPunishmentRepository punishmentRepository;
    private final MySqlSyncEventRepository syncEventRepository;
    private final MySqlReportRepository reportRepository;
    private final RetentionService retentionService;
    private final ConnectionHealthMonitor healthMonitor;

    private final ServerIdentity serverIdentity;
    private final PermissionChecker permissionChecker;
    private final AudienceResolver audienceResolver;
    private final PlayerKicker playerKicker;
    private final MainThreadDispatcher mainThreadDispatcher;

    private final SystemClock clock;
    private final PunishmentStateCache stateCache;
    private final SyncEventConsumer syncEventConsumer;
    private final BanEnforcer banEnforcer;
    private final StaffNotifier staffNotifier;

    private final PunishmentIssueService issueService;
    private final PunishmentRevokeService revokeService;
    private final PunishmentHistoryService historyService;
    private final ReportService reportService;

    private final ScheduledExecutorService backgroundScheduler;

    private SPunishServices(Builder b) {
        this.logger = b.logger;
        this.config = b.config;
        this.reasonsFile = b.reasonsFile;
        this.messagesFile = b.messagesFile;
        this.catalogHolder = b.catalogHolder;
        this.messageService = b.messageService;
        this.guiConfig = b.guiConfig;
        this.connectionProvider = b.connectionProvider;
        this.tableNames = b.tableNames;
        this.ioExecutor = b.ioExecutor;
        this.profileRepository = b.profileRepository;
        this.punishmentRepository = b.punishmentRepository;
        this.syncEventRepository = b.syncEventRepository;
        this.reportRepository = b.reportRepository;
        this.retentionService = b.retentionService;
        this.healthMonitor = b.healthMonitor;
        this.serverIdentity = b.serverIdentity;
        this.permissionChecker = b.permissionChecker;
        this.audienceResolver = b.audienceResolver;
        this.playerKicker = b.playerKicker;
        this.mainThreadDispatcher = b.mainThreadDispatcher;
        this.clock = b.clock;
        this.stateCache = b.stateCache;
        this.syncEventConsumer = b.syncEventConsumer;
        this.banEnforcer = b.banEnforcer;
        this.staffNotifier = b.staffNotifier;
        this.issueService = b.issueService;
        this.revokeService = b.revokeService;
        this.historyService = b.historyService;
        this.reportService = b.reportService;
        this.backgroundScheduler = b.backgroundScheduler;
    }

    public static SPunishServices bootstrap(JavaPlugin plugin) throws SPunishBootstrapException {
        Logger logger = plugin.getLogger();
        Server server = plugin.getServer();
        Path dataFolder = plugin.getDataFolder().toPath();

        SpunishConfig config = loadConfig(dataFolder);
        Path reasonsFile = dataFolder.resolve("reasons.yml");
        ReasonCatalogHolder catalogHolder = loadCatalog(reasonsFile, logger);
        Path messagesFile = dataFolder.resolve("messages.yml");
        MessageService messageService = loadMessages(messagesFile, logger);
        GuiConfig guiConfig = YamlConfigLoader.load(dataFolder.resolve("gui.yml"), "/default/gui.yml", GuiConfig.class);

        DatabaseConnectionProvider connectionProvider = connect(config);
        TableNames tableNames = new TableNames(config.database().tablePrefix());
        migrate(connectionProvider, tableNames, logger);

        IoExecutor ioExecutor = new IoExecutor(Duration.ofMillis(config.database().queryTimeoutMs()));
        MySqlSyncEventRepository syncEventRepository =
                new MySqlSyncEventRepository(connectionProvider.dataSource(), ioExecutor, tableNames);
        MySqlPunishmentRepository punishmentRepository = new MySqlPunishmentRepository(
                connectionProvider.dataSource(), ioExecutor, tableNames, syncEventRepository);
        MySqlProfileRepository profileRepository =
                new MySqlProfileRepository(connectionProvider.dataSource(), ioExecutor, tableNames);
        MySqlReportRepository reportRepository =
                new MySqlReportRepository(connectionProvider.dataSource(), ioExecutor, tableNames);
        RetentionService retentionService = new RetentionService(connectionProvider.dataSource(), ioExecutor, tableNames);

        ConnectionHealthMonitor healthMonitor = new ConnectionHealthMonitor(
                connectionProvider.dataSource(), Duration.ofSeconds(10), Duration.ofMinutes(5), logger);
        healthMonitor.start();

        ServerIdentity serverIdentity = new ConfiguredServerIdentity(config.server().id(), logger);
        PermissionChecker permissionChecker = new PaperPermissionChecker(server);
        AudienceResolver audienceResolver = new PaperAudienceResolver(server);
        PlayerKicker playerKicker = new PaperPlayerKicker(server);
        MainThreadDispatcher mainThreadDispatcher = new PaperMainThreadDispatcher(plugin, server);

        SystemClock clock = new DefaultSystemClock();
        PunishmentStateCache stateCache = new PunishmentStateCache();

        PunishmentIssueService issueService = new PunishmentIssueService(
                punishmentRepository, clock, serverIdentity, permissionChecker, config::durationLimits);
        PunishmentRevokeService revokeService =
                new PunishmentRevokeService(punishmentRepository, clock, serverIdentity, permissionChecker);
        PunishmentHistoryService historyService = new PunishmentHistoryService(punishmentRepository, clock);
        ReportService reportService =
                new ReportService(reportRepository, clock, messageService::zone, config::report);

        BanEnforcer banEnforcer = new BanEnforcer(playerKicker, messageService, clock);
        StaffNotifier staffNotifier = new StaffNotifier(audienceResolver, messageService, clock);
        PaperSyncEventListener syncListener =
                new PaperSyncEventListener(stateCache, banEnforcer, staffNotifier, mainThreadDispatcher);

        SyncEventConsumer syncEventConsumer = new SyncEventConsumer(
                syncEventRepository, punishmentRepository, serverIdentity, clock, syncListener, logger,
                Duration.ofMillis(config.sync().overlapMs()), 10_000);

        ScheduledExecutorService backgroundScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "spunish-background");
            thread.setDaemon(true);
            return thread;
        });
        long pollIntervalMs = Math.max(500, config.sync().pollIntervalMs());
        backgroundScheduler.scheduleWithFixedDelay(
                syncEventConsumer::pollOnce, pollIntervalMs, pollIntervalMs, TimeUnit.MILLISECONDS);

        long housekeepingIntervalMs = Duration.ofMinutes(5).toMillis();
        backgroundScheduler.scheduleWithFixedDelay(() -> runHousekeeping(
                syncEventRepository, retentionService, config, clock, logger),
                housekeepingIntervalMs, housekeepingIntervalMs, TimeUnit.MILLISECONDS);

        Builder builder = new Builder();
        builder.logger = logger;
        builder.config = config;
        builder.reasonsFile = reasonsFile;
        builder.messagesFile = messagesFile;
        builder.catalogHolder = catalogHolder;
        builder.messageService = messageService;
        builder.guiConfig = guiConfig;
        builder.connectionProvider = connectionProvider;
        builder.tableNames = tableNames;
        builder.ioExecutor = ioExecutor;
        builder.profileRepository = profileRepository;
        builder.punishmentRepository = punishmentRepository;
        builder.syncEventRepository = syncEventRepository;
        builder.reportRepository = reportRepository;
        builder.retentionService = retentionService;
        builder.healthMonitor = healthMonitor;
        builder.serverIdentity = serverIdentity;
        builder.permissionChecker = permissionChecker;
        builder.audienceResolver = audienceResolver;
        builder.playerKicker = playerKicker;
        builder.mainThreadDispatcher = mainThreadDispatcher;
        builder.clock = clock;
        builder.stateCache = stateCache;
        builder.syncEventConsumer = syncEventConsumer;
        builder.banEnforcer = banEnforcer;
        builder.staffNotifier = staffNotifier;
        builder.issueService = issueService;
        builder.revokeService = revokeService;
        builder.historyService = historyService;
        builder.reportService = reportService;
        builder.backgroundScheduler = backgroundScheduler;
        return new SPunishServices(builder);
    }

    private static SpunishConfig loadConfig(Path dataFolder) throws SPunishBootstrapException {
        try {
            return YamlConfigLoader.load(dataFolder.resolve("config.yml"), "/default/config.yml", SpunishConfig.class);
        } catch (ConfigLoadException e) {
            throw new SPunishBootstrapException("Could not load config.yml: " + e.getMessage(), e);
        }
    }

    private static ReasonCatalogHolder loadCatalog(Path reasonsFile, Logger logger) throws SPunishBootstrapException {
        ReasonCatalogFile raw;
        try {
            raw = YamlConfigLoader.load(reasonsFile, "/default/reasons.yml", ReasonCatalogFile.class);
        } catch (ConfigLoadException e) {
            throw new SPunishBootstrapException("Could not load reasons.yml: " + e.getMessage(), e);
        }
        CatalogLoadResult result = new ReasonCatalogValidator().buildCatalog(raw);
        if (result instanceof CatalogLoadResult.Failure failure) {
            for (CatalogValidationError error : failure.errors()) {
                logger.severe("reasons.yml: " + error);
            }
            throw new SPunishBootstrapException(
                    "reasons.yml failed validation (" + failure.errors().size() + " error(s); see log above)");
        }
        return new ReasonCatalogHolder(((CatalogLoadResult.Success) result).catalog());
    }

    private static MessageService loadMessages(Path messagesFile, Logger logger) throws SPunishBootstrapException {
        try {
            ConfigurationNode root = YamlConfigLoader.loadNode(messagesFile, "/default/messages.yml");
            ConfigurationNode defaults = YamlConfigLoader.loadBundledDefaults("/default/messages.yml");
            return new MessageService(root, defaults, logger);
        } catch (ConfigLoadException e) {
            throw new SPunishBootstrapException("Could not load messages.yml: " + e.getMessage(), e);
        }
    }

    private static DatabaseConnectionProvider connect(SpunishConfig config) throws SPunishBootstrapException {
        try {
            return new DatabaseConnectionProvider(config.database());
        } catch (RuntimeException e) {
            throw new SPunishBootstrapException("Could not connect to the database: " + e.getMessage(), e);
        }
    }

    private static void migrate(DatabaseConnectionProvider provider, TableNames tables, Logger logger)
            throws SPunishBootstrapException {
        try {
            new SchemaMigrator(provider.dataSource(), tables, logger).migrate();
        } catch (Exception e) {
            provider.close();
            throw new SPunishBootstrapException("Schema migration failed: " + e.getMessage(), e);
        }
    }

    private static void runHousekeeping(
            MySqlSyncEventRepository syncEventRepository, RetentionService retentionService,
            SpunishConfig config, SystemClock clock, Logger logger) {
        try {
            syncEventRepository.deleteOlderThan(clock.now().minusMillis(config.sync().eventRetentionMs())).join();
        } catch (RuntimeException e) {
            logger.warning("Sync event retention cleanup failed: " + e.getMessage());
        }
        if (config.retention().enabled()) {
            try {
                retentionService.purgeClosedOlderThan(config.retention().retentionDays()).join();
            } catch (RuntimeException e) {
                logger.warning("Punishment retention cleanup failed: " + e.getMessage());
            }
        }
    }

    /**
     * The admin reload command only refreshes the reason catalog and
     * messages; other configuration requires a full plugin restart.
     */
    public ReloadOutcome reloadCatalogAndMessages() {
        List<String> errors = new ArrayList<>();
        try {
            ReasonCatalogFile fresh = YamlConfigLoader.load(reasonsFile, "/default/reasons.yml", ReasonCatalogFile.class);
            CatalogLoadResult result = catalogHolder.reload(fresh);
            if (result instanceof CatalogLoadResult.Failure failure) {
                failure.errors().forEach(error -> errors.add("reasons.yml: " + error));
            }
        } catch (ConfigLoadException e) {
            errors.add("reasons.yml: " + e.getMessage());
        }
        try {
            ConfigurationNode fresh = YamlConfigLoader.loadNode(messagesFile, "/default/messages.yml");
            if (!messageService.reload(fresh)) {
                errors.add("messages.yml: invalid content (see server log)");
            }
        } catch (ConfigLoadException e) {
            errors.add("messages.yml: " + e.getMessage());
        }
        return errors.isEmpty() ? ReloadOutcome.success() : ReloadOutcome.failure(errors);
    }

    public Logger logger() {
        return logger;
    }

    public SpunishConfig config() {
        return config;
    }

    public ReasonCatalogHolder catalogHolder() {
        return catalogHolder;
    }

    public MessageService messageService() {
        return messageService;
    }

    public GuiConfig guiConfig() {
        return guiConfig;
    }

    public ProfileRepository profileRepository() {
        return profileRepository;
    }

    public MySqlPunishmentRepository punishmentRepository() {
        return punishmentRepository;
    }

    public ServerIdentity serverIdentity() {
        return serverIdentity;
    }

    public PermissionChecker permissionChecker() {
        return permissionChecker;
    }

    public AudienceResolver audienceResolver() {
        return audienceResolver;
    }

    public MainThreadDispatcher mainThreadDispatcher() {
        return mainThreadDispatcher;
    }

    public SystemClock clock() {
        return clock;
    }

    public PunishmentStateCache stateCache() {
        return stateCache;
    }

    public BanEnforcer banEnforcer() {
        return banEnforcer;
    }

    public StaffNotifier staffNotifier() {
        return staffNotifier;
    }

    public PunishmentIssueService issueService() {
        return issueService;
    }

    public PunishmentRevokeService revokeService() {
        return revokeService;
    }

    public PunishmentHistoryService historyService() {
        return historyService;
    }

    public ReportService reportService() {
        return reportService;
    }

    public ConnectionHealthMonitor healthMonitor() {
        return healthMonitor;
    }

    @Override
    public void close() {
        backgroundScheduler.shutdownNow();
        healthMonitor.close();
        ioExecutor.close();
        connectionProvider.close();
    }

    private static final class Builder {
        Logger logger;
        SpunishConfig config;
        Path reasonsFile;
        Path messagesFile;
        ReasonCatalogHolder catalogHolder;
        MessageService messageService;
        GuiConfig guiConfig;
        DatabaseConnectionProvider connectionProvider;
        TableNames tableNames;
        IoExecutor ioExecutor;
        MySqlProfileRepository profileRepository;
        MySqlPunishmentRepository punishmentRepository;
        MySqlSyncEventRepository syncEventRepository;
        MySqlReportRepository reportRepository;
        RetentionService retentionService;
        ConnectionHealthMonitor healthMonitor;
        ServerIdentity serverIdentity;
        PermissionChecker permissionChecker;
        AudienceResolver audienceResolver;
        PlayerKicker playerKicker;
        MainThreadDispatcher mainThreadDispatcher;
        SystemClock clock;
        PunishmentStateCache stateCache;
        SyncEventConsumer syncEventConsumer;
        BanEnforcer banEnforcer;
        StaffNotifier staffNotifier;
        PunishmentIssueService issueService;
        PunishmentRevokeService revokeService;
        PunishmentHistoryService historyService;
        ReportService reportService;
        ScheduledExecutorService backgroundScheduler;
    }
}
