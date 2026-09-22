package com.spunish.common.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;

public final class MySqlSyncEventRepository implements SyncEventRepository {

    /**
     * Rows deleted per statement, so a large backlog (e.g. cleanup not having run for a
     * while) cannot hold row locks for an extended period in one unbounded {@code DELETE}.
     */
    private static final int DELETE_BATCH_SIZE = 500;

    private final DataSource dataSource;
    private final IoExecutor ioExecutor;
    private final TableNames tables;

    public MySqlSyncEventRepository(DataSource dataSource, IoExecutor ioExecutor, TableNames tables) {
        this.dataSource = dataSource;
        this.ioExecutor = ioExecutor;
        this.tables = tables;
    }

    @Override
    public CompletableFuture<List<SyncEvent>> pollSince(Instant sinceInclusive) {
        return ioExecutor.submit(() -> {
            String sql = "SELECT id, type, punishment_id, target_uuid, origin_server, created_at FROM `"
                    + tables.syncEvents() + "` WHERE created_at >= ? ORDER BY created_at ASC, id ASC";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, Timestamp.from(sinceInclusive));
                List<SyncEvent> events = new ArrayList<>();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        events.add(mapRow(rs));
                    }
                }
                return events;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> deleteOlderThan(Instant threshold) {
        return ioExecutor.submit(() -> {
            String sql = "DELETE FROM `" + tables.syncEvents() + "` WHERE created_at < ? LIMIT ?";
            int totalDeleted = 0;
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, Timestamp.from(threshold));
                statement.setInt(2, DELETE_BATCH_SIZE);
                int deletedInBatch;
                do {
                    deletedInBatch = statement.executeUpdate();
                    totalDeleted += deletedInBatch;
                } while (deletedInBatch == DELETE_BATCH_SIZE);
            }
            return totalDeleted;
        });
    }

    void writeOnConnection(
            Connection connection, SyncEventType type, long punishmentId, UUID targetUuid, String originServer, Instant createdAt)
            throws SQLException {
        String sql = "INSERT INTO `" + tables.syncEvents()
                + "` (type, punishment_id, target_uuid, origin_server, created_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type.name());
            statement.setLong(2, punishmentId);
            statement.setBytes(3, UuidBinary.toBytes(targetUuid));
            statement.setString(4, originServer);
            statement.setTimestamp(5, Timestamp.from(createdAt));
            statement.executeUpdate();
        }
    }

    private static SyncEvent mapRow(ResultSet rs) throws SQLException {
        return new SyncEvent(
                rs.getLong("id"),
                SyncEventType.valueOf(rs.getString("type")),
                rs.getLong("punishment_id"),
                UuidBinary.fromBytes(rs.getBytes("target_uuid")),
                rs.getString("origin_server"),
                rs.getTimestamp("created_at").toInstant());
    }
}
