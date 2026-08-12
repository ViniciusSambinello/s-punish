# Database

MySQL 8.0+ is the only shared state between every backend and the proxy — no
Redis, no plugin messaging channel is required. Table names are prefixed
with `database.table-prefix` from `config.yml` (default `sp_`), so multiple
installs can share one schema.

## Schema

```sql
CREATE TABLE `{p}profiles` (
  `uuid`         BINARY(16)  NOT NULL,
  `name`         VARCHAR(16) NOT NULL,
  `last_seen_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`uuid`),
  KEY `idx_profiles_name` (`name`, `last_seen_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `{p}punishments` (
  `id`             BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `public_id`      CHAR(8)            NOT NULL,
  `category`       ENUM('BAN','MUTE') NOT NULL,
  `target_uuid`    BINARY(16)         NOT NULL,
  `target_name`    VARCHAR(16)        NOT NULL,
  `actor_type`     ENUM('PLAYER','CONSOLE')          NOT NULL,
  `actor_uuid`     BINARY(16)         NULL,
  `actor_name`     VARCHAR(32)        NOT NULL,
  `reason_id`      VARCHAR(64)        NOT NULL,
  `reason_display` VARCHAR(128)       NOT NULL,
  `created_at`     DATETIME(3)        NOT NULL,
  `expires_at`     DATETIME(3)        NULL,
  `origin_server`  VARCHAR(64)        NOT NULL,
  `revoker_type`   ENUM('PLAYER','CONSOLE','SYSTEM') NULL,
  `revoker_uuid`   BINARY(16)         NULL,
  `revoker_name`   VARCHAR(32)        NULL,
  `revoked_at`     DATETIME(3)        NULL,
  `revoke_reason`  VARCHAR(255)       NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_punishments_public_id` (`public_id`),
  KEY `idx_punishments_active` (`target_uuid`, `category`, `revoked_at`, `expires_at`),
  KEY `idx_punishments_history` (`target_uuid`, `created_at`),
  KEY `idx_punishments_report` (`category`, `created_at`, `actor_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `{p}sync_events` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `type`          ENUM('PUNISHMENT_CREATED','PUNISHMENT_REVOKED') NOT NULL,
  `punishment_id` BIGINT UNSIGNED NOT NULL,
  `target_uuid`   BINARY(16)      NOT NULL,
  `origin_server` VARCHAR(64)     NOT NULL,
  `created_at`    DATETIME(3)     NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sync_events_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `{p}schema_version` (
  `version`    INT         NOT NULL,
  `applied_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Why these choices

- **UUIDs as `BINARY(16)`**, not `CHAR(36)` — half the index footprint.
- **Instants as `DATETIME(3)` in UTC**, compared with `UTC_TIMESTAMP(3)` in
  every expiration predicate — the database's clock is authoritative, not
  any individual server's, since backends in a network rarely have
  perfectly synced clocks. If each backend decided expiration by its own
  clock, the same player could be let in on one server and refused on
  another. The application clock is only used for display formatting and to
  compute report window boundaries, which are then sent as parameters.
- **`expires_at IS NULL`** means permanent. **`revoked_at IS NULL`** means
  the original application still stands.
- **`actor_type`/`revoker_type`** exist because a revocation must
  distinguish a person, the console, and the system (an automatic revocation
  from an authorized override) — `NULL` alone can't express that
  three-way distinction.
- **`reason_display` is denormalized** on purpose: the reason catalog can
  rename or remove a reason later, and history must keep showing the text
  that was in force at the moment a punishment was applied, not the
  catalog's current text.
- **`public_id`** (8 characters) is the identifier shown on the rejection
  screen, so staff can look up the exact record without exposing the
  sequential primary key.
- **`idx_punishments_active`** backs the login ban check and the "already
  has an active punishment" lookup. **`idx_punishments_history`** backs
  paginated history (and can also satisfy the login check's
  `ORDER BY created_at DESC LIMIT 1` without a sort, so the query planner
  sometimes prefers it over `idx_punishments_active` — both are real index
  hits, never a full scan). **`idx_punishments_report`** backs the
  aggregate report queries, including the per-staffer ones.

## Migrations

Schema changes are versioned scripts applied inside a named `GET_LOCK`, with
the applied version recorded in `schema_version`. Three backends starting
simultaneously against an empty database all resolve to the same final
version without a duplicate-object error. If `schema_version` already holds
a version newer than the running binary supports, the plugin logs the error
and refuses to enable rather than risk misreading a schema it doesn't
understand.

Only backends run migrations. The Velocity proxy module only ever reads —
install it after at least one backend has already migrated the schema (see
[deployment.md](deployment.md)).

## Retention

Disabled by default (`retention.enabled: false`). When enabled,
`retention.retention-days` in `config.yml` controls how long a **closed**
punishment (revoked, or naturally expired) is kept before being deleted.
An active punishment — unrevoked and unexpired, however old it originally
was applied — is never a candidate for deletion, regardless of how long
retention has been enabled.

`sync_events` rows are pruned independently and much sooner
(`sync.event-retention-ms`, default 5 minutes) — an instance that was
offline longer than that reloads state at startup rather than depend on the
event table to converge.
