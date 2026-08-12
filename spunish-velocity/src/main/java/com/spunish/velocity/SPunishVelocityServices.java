package com.spunish.velocity;

import com.spunish.common.config.ConfigLoadException;
import com.spunish.common.config.SpunishConfig;
import com.spunish.common.config.YamlConfigLoader;
import com.spunish.common.domain.DefaultSystemClock;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.message.MessageService;
import com.spunish.common.platform.AudienceResolver;
import com.spunish.common.platform.ConfiguredServerIdentity;
import com.spunish.common.platform.MainThreadDispatcher;
import com.spunish.common.platform.PermissionChecker;
import com.spunish.common.platform.PlayerKicker;
import com.spunish.common.platform.ServerIdentity;
import com.spunish.common.storage.DatabaseConnectionProvider;
import com.spunish.common.storage.IoExecutor;
import com.spunish.common.storage.MySqlPunishmentRepository;
import com.spunish.common.storage.MySqlSyncEventRepository;
import com.spunish.common.storage.TableNames;
import com.spunish.common.sync.SyncEventConsumer;
import com.spunish.velocity.platform.VelocityAudienceResolver;
import com.spunish.velocity.platform.VelocityMainThreadDispatcher;
import com.spunish.velocity.platform.VelocityPermissionChecker;
import com.spunish.velocity.platform.VelocityPlayerKicker;
import com.spunish.velocity.sync.ProxySyncEventListener;
import com.velocitypowered.api.proxy.ProxyServer;
import org.spongepowered.configurate.ConfigurationNode;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The proxy only checks bans at login and consumes sync events to disconnect
 * a player banned elsewhere; it never issues, revokes, or reports on
 * punishments, and never runs schema migrations — that stays the backend's job.
 */
public final class SPunishVelocityServices implements AutoCloseable {

    private final Logger logger;
    private final SpunishConfig config;
    private final MessageService messageService;
    private final DatabaseConnectionProvider connectionProvider;
    private final IoExecutor ioExecutor;
    private final MySqlPunishmentRepository punishmentRepository;
    private final ServerIdentity serverIdentity;
    private final PermissionChecker permissionChecker;
    private final AudienceResolver audienceResolver;
    private final MainThreadDispatcher mainThreadDispatcher;
    private final SystemClock clock;
    private final SyncEventConsumer syncEventConsumer;
    private final ScheduledExecutorService backgroundScheduler;

    private SPunishVelocityServices(Builder b) {
        this.logger = b.logger;
        this.config = b.config;
        this.messageService = b.messageService;
        this.connectionProvider = b.connectionProvider;
        this.ioExecutor = b.ioExecutor;
        this.punishmentRepository = b.punishmentRepository;
        this.serverIdentity = b.serverIdentity;
        this.permissionChecker = b.permissionChecker;
        this.audienceResolver = b.audienceResolver;
        this.mainThreadDispatcher = b.mainThreadDispatcher;
        this.clock = b.clock;
        this.syncEventConsumer = b.syncEventConsumer;
        this.backgroundScheduler = b.backgroundScheduler;
    }

    public static SPunishVelocityServices bootstrap(ProxyServer server, Path dataDirectory)
            throws SPunishVelocityBootstrapException {
        Logger logger = Logger.getLogger("s-punish");

        SpunishConfig config = loadConfig(dataDirectory);
        MessageService messageService = loadMessages(dataDirectory, logger);

        DatabaseConnectionProvider connectionProvider = connect(config);
        TableNames tableNames = new TableNames(config.database().tablePrefix());
        IoExecutor ioExecutor = new IoExecutor(Duration.ofMillis(config.database().queryTimeoutMs()));
        MySqlSyncEventRepository syncEventRepository =
                new MySqlSyncEventRepository(connectionProvider.dataSource(), ioExecutor, tableNames);
        MySqlPunishmentRepository punishmentRepository = new MySqlPunishmentRepository(
                connectionProvider.dataSource(), ioExecutor, tableNames, syncEventRepository);

        ServerIdentity serverIdentity = new ConfiguredServerIdentity(config.server().id(), logger);
        PermissionChecker permissionChecker = new VelocityPermissionChecker(server);
        AudienceResolver audienceResolver = new VelocityAudienceResolver(server);
        PlayerKicker playerKicker = new VelocityPlayerKicker(server);
        MainThreadDispatcher mainThreadDispatcher = new VelocityMainThreadDispatcher();
        SystemClock clock = new DefaultSystemClock();

        ProxySyncEventListener syncListener = new ProxySyncEventListener(playerKicker, messageService, clock);
        SyncEventConsumer syncEventConsumer = new SyncEventConsumer(
                syncEventRepository, punishmentRepository, serverIdentity, clock, syncListener, logger,
                Duration.ofMillis(config.sync().overlapMs()), 10_000);

        ScheduledExecutorService backgroundScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "spunish-proxy-sync");
            thread.setDaemon(true);
            return thread;
        });
        long pollIntervalMs = Math.max(500, config.sync().pollIntervalMs());
        backgroundScheduler.scheduleWithFixedDelay(
                syncEventConsumer::pollOnce, pollIntervalMs, pollIntervalMs, TimeUnit.MILLISECONDS);

        Builder builder = new Builder();
        builder.logger = logger;
        builder.config = config;
        builder.messageService = messageService;
        builder.connectionProvider = connectionProvider;
        builder.ioExecutor = ioExecutor;
        builder.punishmentRepository = punishmentRepository;
        builder.serverIdentity = serverIdentity;
        builder.permissionChecker = permissionChecker;
        builder.audienceResolver = audienceResolver;
        builder.mainThreadDispatcher = mainThreadDispatcher;
        builder.clock = clock;
        builder.syncEventConsumer = syncEventConsumer;
        builder.backgroundScheduler = backgroundScheduler;
        return new SPunishVelocityServices(builder);
    }

    private static SpunishConfig loadConfig(Path dataDirectory) throws SPunishVelocityBootstrapException {
        try {
            return YamlConfigLoader.load(
                    dataDirectory.resolve("config.yml"), "/default/velocity-config.yml", SpunishConfig.class);
        } catch (ConfigLoadException e) {
            throw new SPunishVelocityBootstrapException("Could not load config.yml: " + e.getMessage(), e);
        }
    }

    private static MessageService loadMessages(Path dataDirectory, Logger logger) throws SPunishVelocityBootstrapException {
        try {
            Path messagesFile = dataDirectory.resolve("messages.yml");
            ConfigurationNode root = YamlConfigLoader.loadNode(messagesFile, "/default/messages.yml");
            ConfigurationNode defaults = YamlConfigLoader.loadBundledDefaults("/default/messages.yml");
            return new MessageService(root, defaults, logger);
        } catch (ConfigLoadException e) {
            throw new SPunishVelocityBootstrapException("Could not load messages.yml: " + e.getMessage(), e);
        }
    }

    private static DatabaseConnectionProvider connect(SpunishConfig config) throws SPunishVelocityBootstrapException {
        try {
            return new DatabaseConnectionProvider(config.database());
        } catch (RuntimeException e) {
            throw new SPunishVelocityBootstrapException("Could not connect to the database: " + e.getMessage(), e);
        }
    }

    public Logger logger() {
        return logger;
    }

    public SpunishConfig config() {
        return config;
    }

    public MessageService messageService() {
        return messageService;
    }

    public MySqlPunishmentRepository punishmentRepository() {
        return punishmentRepository;
    }

    public SystemClock clock() {
        return clock;
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

    @Override
    public void close() {
        backgroundScheduler.shutdownNow();
        ioExecutor.close();
        connectionProvider.close();
    }

    private static final class Builder {
        Logger logger;
        SpunishConfig config;
        MessageService messageService;
        DatabaseConnectionProvider connectionProvider;
        IoExecutor ioExecutor;
        MySqlPunishmentRepository punishmentRepository;
        ServerIdentity serverIdentity;
        PermissionChecker permissionChecker;
        AudienceResolver audienceResolver;
        MainThreadDispatcher mainThreadDispatcher;
        SystemClock clock;
        SyncEventConsumer syncEventConsumer;
        ScheduledExecutorService backgroundScheduler;
    }
}
