package com.spunish.common.storage;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;

public final class MySqlPunishmentRepository implements PunishmentRepository {

    private static final String COLUMNS = PunishmentRowMapper.COLUMNS;

    private static final int MAX_PUBLIC_ID_ATTEMPTS = 5;

    private final DataSource dataSource;
    private final IoExecutor ioExecutor;
    private final TableNames tables;
    private final MySqlSyncEventRepository syncEvents;
    private final PublicIdGenerator publicIdGenerator = new PublicIdGenerator();

    public MySqlPunishmentRepository(
            DataSource dataSource, IoExecutor ioExecutor, TableNames tables, MySqlSyncEventRepository syncEvents) {
        this.dataSource = dataSource;
        this.ioExecutor = ioExecutor;
        this.tables = tables;
        this.syncEvents = syncEvents;
    }

    @Override
    public CompletableFuture<Punishment> insert(InsertPunishmentCommand command) {
        return ioExecutor.submit(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    Punishment applied = insertOnConnection(connection, command);
                    syncEvents.writeOnConnection(connection, SyncEventType.PUNISHMENT_CREATED,
                            applied.id(), applied.targetUuid(), applied.originServer(), applied.createdAt());
                    connection.commit();
                    return applied;
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        });
    }

    @Override
    public CompletableFuture<OverrideResult> insertWithOverride(
            InsertPunishmentCommand newPunishment, long previousPunishmentId, Actor revoker, Instant revokedAt, String revokeReason) {
        return ioExecutor.submit(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    Punishment revoked = revokeOnConnection(connection, previousPunishmentId, revoker, revokedAt, revokeReason);
                    syncEvents.writeOnConnection(connection, SyncEventType.PUNISHMENT_REVOKED,
                            revoked.id(), revoked.targetUuid(), newPunishment.originServer(), revokedAt);

                    Punishment applied = insertOnConnection(connection, newPunishment);
                    syncEvents.writeOnConnection(connection, SyncEventType.PUNISHMENT_CREATED,
                            applied.id(), applied.targetUuid(), applied.originServer(), applied.createdAt());

                    connection.commit();
                    return new OverrideResult(applied, revoked);
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        });
    }

    private Punishment insertOnConnection(Connection connection, InsertPunishmentCommand command) throws SQLException {
        if (command.actor() instanceof com.spunish.common.domain.SystemActor) {
            throw new IllegalArgumentException("A punishment cannot be applied by the system actor");
        }
        ActorColumns.Mapped actor = ActorColumns.from(command.actor());
        String sql = "INSERT INTO `" + tables.punishments() + "` "
                + "(public_id, category, target_uuid, target_name, actor_type, actor_uuid, actor_name, "
                + "reason_id, reason_display, created_at, expires_at, origin_server) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

        SQLException lastDuplicate = null;
        for (int attempt = 0; attempt < MAX_PUBLIC_ID_ATTEMPTS; attempt++) {
            String publicId = publicIdGenerator.generate();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, publicId);
                statement.setString(2, command.category().name());
                statement.setBytes(3, UuidBinary.toBytes(command.targetUuid()));
                statement.setString(4, command.targetName());
                statement.setString(5, actor.type());
                setNullableBytes(statement, 6, actor.uuid());
                statement.setString(7, actor.name());
                statement.setString(8, command.reasonId());
                statement.setString(9, command.reasonDisplay());
                statement.setTimestamp(10, Timestamp.from(command.createdAt()));
                setNullableTimestamp(statement, 11, command.expiresAt());
                statement.setString(12, command.originServer());
                statement.executeUpdate();

                long id;
                try (ResultSet generated = statement.getGeneratedKeys()) {
                    generated.next();
                    id = generated.getLong(1);
                }
                return new Punishment(
                        id, publicId, command.category(), command.targetUuid(), command.targetName(),
                        command.actor(), command.reasonId(), command.reasonDisplay(), command.createdAt(),
                        command.expiresAt(), command.originServer(), null, null, null);
            } catch (SQLException e) {
                if (!"23000".equals(e.getSQLState())) {
                    throw e;
                }
                lastDuplicate = e;
            }
        }
        throw new SQLException(
                "Could not generate a unique public_id after " + MAX_PUBLIC_ID_ATTEMPTS + " attempts", lastDuplicate);
    }

    @Override
    public CompletableFuture<Optional<Punishment>> findActive(UUID targetUuid, PunishmentCategory category) {
        return ioExecutor.submit(() -> {
            String sql = "SELECT " + COLUMNS + " FROM `" + tables.punishments() + "` "
                    + "WHERE target_uuid = ? AND category = ? AND revoked_at IS NULL "
                    + "AND (expires_at IS NULL OR expires_at > UTC_TIMESTAMP(3)) "
                    + "ORDER BY created_at DESC LIMIT 1";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBytes(1, UuidBinary.toBytes(targetUuid));
                statement.setString(2, category.name());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? Optional.of(PunishmentRowMapper.mapRow(rs)) : Optional.<Punishment>empty();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Punishment>> findById(long id) {
        return ioExecutor.submit(() -> findOneBy("id = ?", id));
    }

    @Override
    public CompletableFuture<Optional<Punishment>> findByPublicId(String publicId) {
        return ioExecutor.submit(() -> findOneBy("public_id = ?", publicId));
    }

    private Optional<Punishment> findOneBy(String predicate, Object value) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM `" + tables.punishments() + "` WHERE " + predicate;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(PunishmentRowMapper.mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public CompletableFuture<Boolean> revoke(
            long punishmentId, Actor revoker, Instant revokedAt, String revokeReason, String originServer) {
        return ioExecutor.submit(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    Punishment revoked = revokeOnConnection(connection, punishmentId, revoker, revokedAt, revokeReason);
                    syncEvents.writeOnConnection(connection, SyncEventType.PUNISHMENT_REVOKED,
                            revoked.id(), revoked.targetUuid(), originServer, revokedAt);
                    connection.commit();
                    return true;
                } catch (AlreadyRevokedOrMissingException notApplied) {
                    connection.rollback();
                    return false;
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        });
    }

    private Punishment revokeOnConnection(
            Connection connection, long punishmentId, Actor revoker, Instant revokedAt, String revokeReason) throws SQLException {
        ActorColumns.Mapped mapped = ActorColumns.from(revoker);
        String updateSql = "UPDATE `" + tables.punishments() + "` SET "
                + "revoker_type = ?, revoker_uuid = ?, revoker_name = ?, revoked_at = ?, revoke_reason = ? "
                + "WHERE id = ? AND revoked_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setString(1, mapped.type());
            setNullableBytes(statement, 2, mapped.uuid());
            statement.setString(3, mapped.name());
            statement.setTimestamp(4, Timestamp.from(revokedAt));
            if (revokeReason == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, revokeReason);
            }
            statement.setLong(6, punishmentId);
            if (statement.executeUpdate() != 1) {
                throw new AlreadyRevokedOrMissingException(punishmentId);
            }
        }
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT " + COLUMNS + " FROM `" + tables.punishments() + "` WHERE id = ?")) {
            select.setLong(1, punishmentId);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return PunishmentRowMapper.mapRow(rs);
            }
        }
    }

    private static final class AlreadyRevokedOrMissingException extends SQLException {

        private static final long serialVersionUID = 1L;

        AlreadyRevokedOrMissingException(long punishmentId) {
            super("Punishment " + punishmentId + " was already revoked or does not exist");
        }
    }

    @Override
    public CompletableFuture<List<Punishment>> findHistory(
            UUID targetUuid, Optional<PunishmentCategory> category, int limit, int offset) {
        return ioExecutor.submit(() -> {
            StringBuilder sql = new StringBuilder(
                    "SELECT " + COLUMNS + " FROM `" + tables.punishments() + "` WHERE target_uuid = ?");
            category.ifPresent(c -> sql.append(" AND category = ?"));
            sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                int index = 1;
                statement.setBytes(index++, UuidBinary.toBytes(targetUuid));
                if (category.isPresent()) {
                    statement.setString(index++, category.get().name());
                }
                statement.setInt(index++, Math.max(0, limit));
                statement.setInt(index, Math.max(0, offset));

                List<Punishment> results = new ArrayList<>();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        results.add(PunishmentRowMapper.mapRow(rs));
                    }
                }
                return results;
            }
        });
    }

    private static void setNullableBytes(PreparedStatement statement, int index, byte[] value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BINARY);
        } else {
            statement.setBytes(index, value);
        }
    }

    private static void setNullableTimestamp(PreparedStatement statement, int index, Instant value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.from(value));
        }
    }

}
