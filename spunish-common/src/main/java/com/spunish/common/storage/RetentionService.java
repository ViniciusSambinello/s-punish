package com.spunish.common.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;

/**
 * Deletes closed (revoked or naturally expired) punishments past the
 * configured retention window. Retention is disabled by default
 * ({@code retention.enabled} defaults to {@code false}).
 */
public final class RetentionService {

    /**
     * Rows deleted per statement. A single unbounded {@code DELETE} over a
     * table that has accumulated years of history (retention is disabled by
     * default, so a first run can face a large backlog) would hold its
     * row locks for as long as the whole delete takes, and cannot be
     * interrupted — this instead makes progress in small, quickly-committed
     * steps that let other transactions interleave between batches.
     */
    private static final int BATCH_SIZE = 500;

    private final DataSource dataSource;
    private final IoExecutor ioExecutor;
    private final TableNames tables;

    public RetentionService(DataSource dataSource, IoExecutor ioExecutor, TableNames tables) {
        this.dataSource = dataSource;
        this.ioExecutor = ioExecutor;
        this.tables = tables;
    }

    /**
     * A punishment is eligible once it has been closed — revoked, or
     * naturally expired — for longer than {@code retentionDays}. An active
     * punishment (unrevoked, unexpired) can never match this, regardless of
     * how long ago it was originally applied.
     *
     * <p>Deletes in batches of {@value #BATCH_SIZE} rather than a single
     * statement so a large backlog cannot hold row locks for an extended
     * period or block this scheduled task's thread for too long; each batch
     * commits (autocommit) independently, so progress made before a timeout
     * or failure is never lost.
     */
    public CompletableFuture<Integer> purgeClosedOlderThan(int retentionDays) {
        return ioExecutor.submit(() -> {
            String sql = "DELETE FROM `" + tables.punishments() + "` WHERE "
                    + "(revoked_at IS NOT NULL AND revoked_at <= UTC_TIMESTAMP(3) - INTERVAL ? DAY) OR "
                    + "(revoked_at IS NULL AND expires_at IS NOT NULL AND expires_at <= UTC_TIMESTAMP(3) - INTERVAL ? DAY) "
                    + "LIMIT ?";
            int totalPurged = 0;
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(3, BATCH_SIZE);
                int deletedInBatch;
                do {
                    statement.setInt(1, retentionDays);
                    statement.setInt(2, retentionDays);
                    deletedInBatch = statement.executeUpdate();
                    totalPurged += deletedInBatch;
                } while (deletedInBatch == BATCH_SIZE);
            }
            return totalPurged;
        });
    }
}
