package com.spunish.common.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Periodically validates the pool and exposes a queryable health state.
 * HikariCP already evicts broken physical connections and lazily opens new
 * ones once the database is reachable again — this class does not duplicate
 * that; it only detects the outage, backs off the *check* interval (not
 * connection attempts) while unhealthy so a down database isn't hammered,
 * and logs the recovery transition.
 */
public final class ConnectionHealthMonitor implements AutoCloseable {

    private final DataSource dataSource;
    private final Duration baseInterval;
    private final Duration maxInterval;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;

    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private final AtomicReference<Instant> lastCheckedAt = new AtomicReference<>();
    private volatile Duration currentInterval;

    public ConnectionHealthMonitor(DataSource dataSource, Duration baseInterval, Duration maxInterval, Logger logger) {
        this.dataSource = dataSource;
        this.baseInterval = baseInterval;
        this.maxInterval = maxInterval;
        this.logger = logger;
        this.currentInterval = baseInterval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "spunish-connection-health");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        scheduleNext();
    }

    private void scheduleNext() {
        scheduler.schedule(this::checkAndReschedule, currentInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void checkAndReschedule() {
        check();
        scheduleNext();
    }

    /**
     * Package-visible so tests can drive checks deterministically instead of
     * waiting on the real schedule.
     */
    void check() {
        lastCheckedAt.set(Instant.now());
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                onSuccess();
            } else {
                onFailure("Connection reported invalid");
            }
        } catch (SQLException e) {
            onFailure(e.getMessage());
        }
    }

    private void onSuccess() {
        boolean wasUnhealthy = !healthy.getAndSet(true);
        lastError.set(null);
        currentInterval = baseInterval;
        if (wasUnhealthy) {
            logger.info("Database connection recovered.");
        }
    }

    private void onFailure(String message) {
        boolean wasHealthy = healthy.getAndSet(false);
        lastError.set(message);
        if (wasHealthy) {
            logger.warning("Database connection lost: " + message);
        }
        Duration doubled = currentInterval.multipliedBy(2);
        currentInterval = doubled.compareTo(maxInterval) > 0 ? maxInterval : doubled;
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    public Optional<String> lastError() {
        return Optional.ofNullable(lastError.get());
    }

    public Optional<Instant> lastCheckedAt() {
        return Optional.ofNullable(lastCheckedAt.get());
    }

    Duration currentInterval() {
        return currentInterval;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
