package com.spunish.common.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;

/**
 * Deletes closed (revoked or naturally expired) punishments past the
 * configured retention window. Whether this ever runs — {@code retention.enabled}
 * defaults to {@code false} — is the caller's decision, not this class's;
 * this only implements "purge", never "should I purge".
 */
public final class RetentionService {

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
     */
    public CompletableFuture<Integer> purgeClosedOlderThan(int retentionDays) {
        return ioExecutor.submit(() -> {
            String sql = "DELETE FROM `" + tables.punishments() + "` WHERE "
                    + "(revoked_at IS NOT NULL AND revoked_at <= UTC_TIMESTAMP(3) - INTERVAL ? DAY) OR "
                    + "(revoked_at IS NULL AND expires_at IS NOT NULL AND expires_at <= UTC_TIMESTAMP(3) - INTERVAL ? DAY)";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, retentionDays);
                statement.setInt(2, retentionDays);
                return statement.executeUpdate();
            }
        });
    }
}
