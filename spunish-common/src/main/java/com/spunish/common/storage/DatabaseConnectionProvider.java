package com.spunish.common.storage;

import com.spunish.common.config.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Owns the HikariCP pool. {@code driverClassName} is set explicitly to the
 * driver's original FQCN rather than left to {@code ServiceLoader} discovery:
 * once spunish-paper/spunish-velocity are shaded (task 11.1), Shadow's
 * relocator rewrites this exact string constant along with the driver
 * classes themselves, so this line stays correct in both the unshaded test
 * classpath and the packaged plugin jar without any conditional logic.
 */
public final class DatabaseConnectionProvider implements AutoCloseable {

    private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    private final HikariDataSource dataSource;

    public DatabaseConnectionProvider(DatabaseSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(DRIVER_CLASS_NAME);
        config.setJdbcUrl(buildJdbcUrl(settings));
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.pool().maximumPoolSize());
        config.setMinimumIdle(settings.pool().minimumIdle());
        config.setConnectionTimeout(settings.pool().connectionTimeoutMs());
        config.setMaxLifetime(settings.pool().maxLifetimeMs());
        config.setPoolName("spunish");
        this.dataSource = new HikariDataSource(config);
    }

    private static String buildJdbcUrl(DatabaseSettings settings) {
        // Credentials go through setUsername/setPassword, never embedded in the URL,
        // so the URL itself is always safe to include in a log line.
        return "jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.database()
                + "?useSSL=" + settings.useSsl()
                + "&serverTimezone=UTC"
                + "&characterEncoding=utf8";
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public boolean isRunning() {
        return dataSource.isRunning();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
