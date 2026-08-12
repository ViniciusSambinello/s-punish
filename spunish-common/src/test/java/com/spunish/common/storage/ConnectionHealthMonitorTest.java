package com.spunish.common.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.logging.Logger;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionHealthMonitorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private final Logger logger = Logger.getLogger("ConnectionHealthMonitorTest");

    @Test
    void startsHealthy() {
        ConnectionHealthMonitor monitor = new ConnectionHealthMonitor(
                dataSource, Duration.ofSeconds(1), Duration.ofSeconds(30), logger);

        assertThat(monitor.isHealthy()).isTrue();
        monitor.close();
    }

    @Test
    void failedCheckMarksUnhealthyAndRecordsError() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));
        ConnectionHealthMonitor monitor = new ConnectionHealthMonitor(
                dataSource, Duration.ofSeconds(1), Duration.ofSeconds(30), logger);

        monitor.check();

        assertThat(monitor.isHealthy()).isFalse();
        assertThat(monitor.lastError()).contains("connection refused");
        assertThat(monitor.lastCheckedAt()).isPresent();
        monitor.close();
    }

    @Test
    void checkIntervalDoublesOnRepeatedFailureUpToMax() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));
        ConnectionHealthMonitor monitor = new ConnectionHealthMonitor(
                dataSource, Duration.ofSeconds(1), Duration.ofSeconds(5), logger);

        monitor.check();
        assertThat(monitor.currentInterval()).isEqualTo(Duration.ofSeconds(2));
        monitor.check();
        assertThat(monitor.currentInterval()).isEqualTo(Duration.ofSeconds(4));
        monitor.check();
        assertThat(monitor.currentInterval()).isEqualTo(Duration.ofSeconds(5)); // capped at max
        monitor.close();
    }

    @Test
    void recoveryResetsIntervalAndClearsError() throws SQLException {
        when(dataSource.getConnection())
                .thenThrow(new SQLException("down"))
                .thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        ConnectionHealthMonitor monitor = new ConnectionHealthMonitor(
                dataSource, Duration.ofSeconds(1), Duration.ofSeconds(30), logger);

        monitor.check();
        assertThat(monitor.isHealthy()).isFalse();

        monitor.check();
        assertThat(monitor.isHealthy()).isTrue();
        assertThat(monitor.lastError()).isEmpty();
        assertThat(monitor.currentInterval()).isEqualTo(Duration.ofSeconds(1));
        monitor.close();
    }
}
