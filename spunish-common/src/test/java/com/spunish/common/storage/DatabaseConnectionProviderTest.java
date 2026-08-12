package com.spunish.common.storage;

import com.spunish.common.config.DatabaseSettings;
import com.spunish.common.config.PoolSettings;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectionProviderTest {

    private static final String SECRET_PASSWORD = "sup3r-secret-p@ss";

    private static DatabaseSettings settings(String host) {
        return new DatabaseSettings(
                host, 3306, "spunish", "spunish", SECRET_PASSWORD, "sp_", false,
                new PoolSettings(2, 1, 300, 60_000), 300);
    }

    @Test
    void toStringNeverIncludesThePassword() {
        String rendered = settings("localhost").toString();

        assertThat(rendered).doesNotContain(SECRET_PASSWORD);
        assertThat(rendered).contains("REDACTED");
    }

    @Test
    void connectionFailureMessageAndStackTraceNeverIncludeThePassword() {
        // The plugin must refuse to enable at startup when the database is
        // unreachable, rather than failing lazily on first use.
        DatabaseSettings failingSettings = new DatabaseSettings(
                "127.0.0.1", 1, "spunish", "spunish", SECRET_PASSWORD, "sp_", false,
                new PoolSettings(1, 1, 500, 60_000), 300);

        Throwable failure = catchThrowable(() -> {
            try (DatabaseConnectionProvider provider = new DatabaseConnectionProvider(failingSettings)) {
                provider.dataSource().getConnection();
            }
        });

        assertThat(failure).isNotNull();
        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));

        assertThat(failure.getMessage()).doesNotContain(SECRET_PASSWORD);
        assertThat(trace.toString()).doesNotContain(SECRET_PASSWORD);
    }

    private static Throwable catchThrowable(ThrowingCall call) {
        try {
            call.call();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void call() throws SQLException;
    }
}
