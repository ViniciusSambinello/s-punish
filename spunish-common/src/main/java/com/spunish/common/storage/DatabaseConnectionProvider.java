package com.spunish.common.storage;

import com.spunish.common.config.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

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
