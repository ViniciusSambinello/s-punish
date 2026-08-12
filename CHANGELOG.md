# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-08-12

### Added

- Network-wide punishment system for Paper backends behind a Velocity proxy,
  with MySQL as the sole shared source of truth.
- `BAN` and `MUTE` categories with a configurable, per-category reason
  catalog (id, display name, duration, icon, optional restricting
  permission).
- `/punish` in three forms (category GUI, reason GUI, direct apply),
  `/unban`/`/unmute` for manual revocation, `/record` for audit reporting
  (daily/weekly/monthly/all-time windows, staff-wide or per-staffer),
  `/history` for a player's punishment history, and `/spunishreload` for an
  atomic reload of the reason catalog and messages.
- In-game GUIs for category/reason selection, paginated history with a
  category filter and per-punishment detail, and audit reports with a staff
  ranking and reason distribution.
- Proxy-edge ban enforcement: the Velocity module refuses a banned player's
  login before they ever reach a backend, and disconnects an already-online
  player banned elsewhere on the network within the sync propagation window.
- Fully configurable, MiniMessage-formatted messages and GUI text
  (`messages.yml`), reloadable at runtime without a restart.
- Configurable fail-open/fail-closed behavior for login and chat when
  storage is unreachable.
- Fail-fast MySQL connection pooling (HikariCP), versioned schema migrations
  with concurrent-startup safety, and a disabled-by-default retention policy
  for closed punishments.
- Shaded, relocated platform jars for both Paper and Velocity, with a smoke
  test proving the relocated MySQL driver opens a real connection.

[Unreleased]: https://github.com/ViniciusSambinello/s-punish/compare/v0.1.0...develop
[0.1.0]: https://github.com/ViniciusSambinello/s-punish/releases/tag/v0.1.0
