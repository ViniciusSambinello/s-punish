package com.spunish.common.storage;

import java.util.List;

final class Migrations {

    static final int CURRENT_VERSION = 1;

    private Migrations() {
    }

    static List<String> statementsFor(int version, TableNames tables) {
        return switch (version) {
            case 1 -> v1(tables);
            default -> throw new IllegalArgumentException("No migration defined for version " + version);
        };
    }

    private static List<String> v1(TableNames tables) {
        return List.of(
                """
                CREATE TABLE `%s` (
                  `uuid`         BINARY(16)  NOT NULL,
                  `name`         VARCHAR(16) NOT NULL,
                  `last_seen_at` DATETIME(3) NOT NULL,
                  PRIMARY KEY (`uuid`),
                  KEY `idx_profiles_name` (`name`, `last_seen_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.formatted(tables.profiles()),

                """
                CREATE TABLE `%s` (
                  `id`             BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
                  `public_id`      CHAR(8)            NOT NULL,
                  `category`       ENUM('BAN','MUTE') NOT NULL,
                  `target_uuid`    BINARY(16)         NOT NULL,
                  `target_name`    VARCHAR(16)        NOT NULL,
                  `actor_type`     ENUM('PLAYER','CONSOLE')            NOT NULL,
                  `actor_uuid`     BINARY(16)         NULL,
                  `actor_name`     VARCHAR(32)        NOT NULL,
                  `reason_id`      VARCHAR(64)        NOT NULL,
                  `reason_display` VARCHAR(128)       NOT NULL,
                  `created_at`     DATETIME(3)        NOT NULL,
                  `expires_at`     DATETIME(3)        NULL,
                  `origin_server`  VARCHAR(64)        NOT NULL,
                  `revoker_type`   ENUM('PLAYER','CONSOLE','SYSTEM')   NULL,
                  `revoker_uuid`   BINARY(16)         NULL,
                  `revoker_name`   VARCHAR(32)        NULL,
                  `revoked_at`     DATETIME(3)        NULL,
                  `revoke_reason`  VARCHAR(255)       NULL,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uq_punishments_public_id` (`public_id`),
                  KEY `idx_punishments_active` (`target_uuid`, `category`, `revoked_at`, `expires_at`),
                  KEY `idx_punishments_history` (`target_uuid`, `created_at`),
                  KEY `idx_punishments_report` (`category`, `created_at`, `actor_uuid`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.formatted(tables.punishments()),

                """
                CREATE TABLE `%s` (
                  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                  `type`          ENUM('PUNISHMENT_CREATED','PUNISHMENT_REVOKED') NOT NULL,
                  `punishment_id` BIGINT UNSIGNED NOT NULL,
                  `target_uuid`   BINARY(16)      NOT NULL,
                  `origin_server` VARCHAR(64)     NOT NULL,
                  `created_at`    DATETIME(3)     NOT NULL,
                  PRIMARY KEY (`id`),
                  KEY `idx_sync_events_created_at` (`created_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.formatted(tables.syncEvents()),

                """
                CREATE TABLE `%s` (
                  `version`    INT         NOT NULL,
                  `applied_at` DATETIME(3) NOT NULL,
                  PRIMARY KEY (`version`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.formatted(tables.schemaVersion()));
    }
}
