package com.spunish.common.storage;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;

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

public final class MySqlReportRepository implements ReportRepository {

    private final DataSource dataSource;
    private final IoExecutor ioExecutor;
    private final TableNames tables;

    public MySqlReportRepository(DataSource dataSource, IoExecutor ioExecutor, TableNames tables) {
        this.dataSource = dataSource;
        this.ioExecutor = ioExecutor;
        this.tables = tables;
    }

    @Override
    public CompletableFuture<Long> countTotal(PunishmentCategory category, Instant from, Instant to, UUID staffUuid) {
        Filter filter = new Filter(category, from, to, staffUuid);
        return ioExecutor.submit(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tables.punishments() + "` WHERE " + filter.whereClause();
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                filter.bind(statement, 1);
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    @Override
    public CompletableFuture<StateBreakdown> countByState(
            PunishmentCategory category, Instant from, Instant to, UUID staffUuid) {
        Filter filter = new Filter(category, from, to, staffUuid);
        return ioExecutor.submit(() -> {
            String sql = "SELECT "
                    + "SUM(CASE WHEN revoked_at IS NOT NULL THEN 1 ELSE 0 END) AS revoked_count, "
                    + "SUM(CASE WHEN revoked_at IS NULL AND expires_at IS NOT NULL AND expires_at <= UTC_TIMESTAMP(3) "
                    + "     THEN 1 ELSE 0 END) AS expired_count, "
                    + "SUM(CASE WHEN revoked_at IS NULL AND (expires_at IS NULL OR expires_at > UTC_TIMESTAMP(3)) "
                    + "     THEN 1 ELSE 0 END) AS active_count "
                    + "FROM `" + tables.punishments() + "` WHERE " + filter.whereClause();
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                filter.bind(statement, 1);
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    return new StateBreakdown(rs.getLong("active_count"), rs.getLong("expired_count"), rs.getLong("revoked_count"));
                }
            }
        });
    }

    @Override
    public CompletableFuture<List<StaffRanking>> rankByStaff(PunishmentCategory category, Instant from, Instant to, int limit) {
        Filter filter = new Filter(category, from, to, null);
        return ioExecutor.submit(() -> {
            String sql = "SELECT actor_uuid, MAX(actor_name) AS actor_name, COUNT(*) AS cnt FROM `"
                    + tables.punishments() + "` WHERE " + filter.whereClause() + " AND actor_type = 'PLAYER' "
                    + "GROUP BY actor_uuid ORDER BY cnt DESC LIMIT ?";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = filter.bind(statement, 1);
                statement.setInt(index, Math.max(0, limit));
                List<StaffRanking> ranking = new ArrayList<>();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        ranking.add(new StaffRanking(
                                UuidBinary.fromBytes(rs.getBytes("actor_uuid")), rs.getString("actor_name"), rs.getLong("cnt")));
                    }
                }
                return ranking;
            }
        });
    }

    @Override
    public CompletableFuture<List<ReasonDistributionEntry>> reasonDistribution(
            PunishmentCategory category, Instant from, Instant to, UUID staffUuid) {
        Filter filter = new Filter(category, from, to, staffUuid);
        return ioExecutor.submit(() -> {
            String sql = "SELECT reason_id, MAX(reason_display) AS reason_display, COUNT(*) AS cnt "
                    + "FROM `" + tables.punishments() + "` WHERE " + filter.whereClause()
                    + " GROUP BY reason_id ORDER BY cnt DESC";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                filter.bind(statement, 1);
                List<ReasonDistributionEntry> distribution = new ArrayList<>();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        distribution.add(new ReasonDistributionEntry(
                                rs.getString("reason_id"), rs.getString("reason_display"), rs.getLong("cnt")));
                    }
                }
                return distribution;
            }
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> mostRecentByStaff(
            PunishmentCategory category, Instant from, Instant to, UUID staffUuid, int limit) {
        Filter filter = new Filter(category, from, to, staffUuid);
        return ioExecutor.submit(() -> {
            String sql = "SELECT " + PunishmentRowMapper.COLUMNS + " FROM `" + tables.punishments() + "` WHERE "
                    + filter.whereClause() + " ORDER BY created_at DESC LIMIT ?";
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = filter.bind(statement, 1);
                statement.setInt(index, Math.max(0, limit));
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

    private record Filter(PunishmentCategory category, Instant from, Instant to, UUID staffUuid) {

        String whereClause() {
            StringBuilder sql = new StringBuilder("category = ? AND created_at <= ?");
            if (from != null) {
                sql.append(" AND created_at >= ?");
            }
            if (staffUuid != null) {
                sql.append(" AND actor_uuid = ?");
            }
            return sql.toString();
        }

        int bind(PreparedStatement statement, int startIndex) throws SQLException {
            int index = startIndex;
            statement.setString(index++, category.name());
            statement.setTimestamp(index++, Timestamp.from(to));
            if (from != null) {
                statement.setTimestamp(index++, Timestamp.from(from));
            }
            if (staffUuid != null) {
                statement.setBytes(index++, UuidBinary.toBytes(staffUuid));
            }
            return index;
        }
    }
}
