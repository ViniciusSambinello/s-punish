package com.spunish.common.storage;

import com.spunish.common.domain.PlayerProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;

public final class MySqlProfileRepository implements ProfileRepository {

    private final DataSource dataSource;
    private final IoExecutor ioExecutor;
    private final TableNames tables;

    public MySqlProfileRepository(DataSource dataSource, IoExecutor ioExecutor, TableNames tables) {
        this.dataSource = dataSource;
        this.ioExecutor = ioExecutor;
        this.tables = tables;
    }

    @Override
    public CompletableFuture<Void> upsert(PlayerProfile profile) {
        return ioExecutor.submit(() -> {
            String sql = "INSERT INTO `" + tables.profiles() + "` (uuid, name, last_seen_at) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE name = ?, last_seen_at = ?";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBytes(1, UuidBinary.toBytes(profile.uuid()));
                statement.setString(2, profile.name());
                statement.setTimestamp(3, Timestamp.from(profile.lastSeenAt()));
                statement.setString(4, profile.name());
                statement.setTimestamp(5, Timestamp.from(profile.lastSeenAt()));
                statement.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> findByUuid(UUID uuid) {
        return ioExecutor.submit(() -> {
            String sql = "SELECT uuid, name, last_seen_at FROM `" + tables.profiles() + "` WHERE uuid = ?";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBytes(1, UuidBinary.toBytes(uuid));
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.<PlayerProfile>empty();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> findByName(String name) {
        return ioExecutor.submit(() -> {
            String sql = "SELECT uuid, name, last_seen_at FROM `" + tables.profiles()
                    + "` WHERE name = ? ORDER BY last_seen_at DESC LIMIT 1";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.<PlayerProfile>empty();
                }
            }
        });
    }

    private static PlayerProfile mapRow(ResultSet rs) throws SQLException {
        return new PlayerProfile(
                UuidBinary.fromBytes(rs.getBytes("uuid")),
                rs.getString("name"),
                rs.getTimestamp("last_seen_at").toInstant());
    }
}
