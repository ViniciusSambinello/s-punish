package com.spunish.common.storage;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Shared column list and row mapping for every query that returns full
 * {@link Punishment} rows — used by both the CRUD repository and the report
 * repository's "most recent by staffer" query.
 */
final class PunishmentRowMapper {

    static final String COLUMNS = """
            id, public_id, category, target_uuid, target_name, actor_type, actor_uuid, actor_name,
            reason_id, reason_display, created_at, expires_at, origin_server,
            revoker_type, revoker_uuid, revoker_name, revoked_at, revoke_reason""";

    private PunishmentRowMapper() {
    }

    static Punishment mapRow(ResultSet rs) throws SQLException {
        Actor actor = ActorColumns.toActor(rs.getString("actor_type"), rs.getBytes("actor_uuid"), rs.getString("actor_name"));

        String revokerType = rs.getString("revoker_type");
        Actor revoker = revokerType == null
                ? null
                : ActorColumns.toActor(revokerType, rs.getBytes("revoker_uuid"), rs.getString("revoker_name"));

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        Timestamp revokedAt = rs.getTimestamp("revoked_at");

        return new Punishment(
                rs.getLong("id"),
                rs.getString("public_id"),
                PunishmentCategory.valueOf(rs.getString("category")),
                UuidBinary.fromBytes(rs.getBytes("target_uuid")),
                rs.getString("target_name"),
                actor,
                rs.getString("reason_id"),
                rs.getString("reason_display"),
                rs.getTimestamp("created_at").toInstant(),
                expiresAt == null ? null : expiresAt.toInstant(),
                rs.getString("origin_server"),
                revoker,
                revokedAt == null ? null : revokedAt.toInstant(),
                rs.getString("revoke_reason"));
    }
}
