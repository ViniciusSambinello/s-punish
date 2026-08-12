# s-punish

[![CI](https://github.com/ViniciusSambinello/s-punish/actions/workflows/ci.yml/badge.svg)](https://github.com/ViniciusSambinello/s-punish/actions/workflows/ci.yml)

A network-wide punishment system for Paper servers behind a Velocity proxy, with MySQL persistence, a configurable reason catalog, in-game GUIs, and permission-gated audit reporting.

## Why

Vanilla `/ban` and `/mute` write to local files, don't sync across a network's backends, don't record who punished whom or why, and offer no way to audit staff activity. s-punish replaces that with a single source of truth shared by every server on the network, a full audit trail, and configurable reasons/durations/messages.

## Features

- **Two categories** — `BAN` and `MUTE` — each with a configurable reason catalog (id, display name, duration, icon, optional restricting permission).
- **Works for online and offline targets** — punishing a name that has connected to the network before works even while they're offline.
- **Network-wide enforcement** — MySQL is the only shared state; every backend and the proxy converge on the same active punishments within a few seconds, without Redis or plugin messaging.
- **Proxy-edge blocking** — a banned player is refused at the Velocity login step, before ever reaching a backend.
- **In-game GUIs** for category/reason selection, paginated history with a category filter, and audit reports with four time windows (daily/weekly/monthly/all-time), a staff ranking, and per-staffer reports.
- **Fully configurable text** — every message, GUI title, and item label is a MiniMessage string in `messages.yml`, reloadable without a restart.
- **Configurable fail-open/fail-closed behavior** when storage is unreachable, independently for login and for chat.

## Requirements

- Java 25 on every Paper backend and on the Velocity proxy.
- Paper `1.26.2` (API `io.papermc.paper:paper-api:26.2.build.112-stable`).
- Velocity `4.0.0` (proxy module is optional but recommended — see [Installation](#installation)).
- MySQL 8.0+, reachable from every backend and from the proxy.

## Installation

1. Provision the MySQL database and an application user; confirm it's reachable from every backend and from the proxy.
2. Drop `spunish-paper-<version>.jar` into one backend's `plugins/` folder and start it. The schema is created automatically on first boot.
3. Edit `plugins/s-punish/config.yml` with your database credentials, then restart that backend and confirm punishments, expiration, history and reports work as expected.
4. Copy the same `config.yml` to every other backend, changing only `server.id` on each.
5. Optionally drop `spunish-velocity-<version>.jar` into the proxy's `plugins/` folder — backends already enforce bans on their own, so the proxy module is reinforcement, not a prerequisite.
6. Deny the vanilla punishment commands (`minecraft.command.ban`, `minecraft.command.pardon`, `minecraft.command.ban-ip`, etc.) to staff, so s-punish is the only path.

See [docs/deployment.md](docs/deployment.md) for the full deployment order and pool-sizing guidance.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/punish <player>` | Opens the category GUI. | `spunish.punish` |
| `/punish <player> <category>` | Opens the reason GUI for that category directly. | `spunish.punish`, `spunish.punish.<category>` |
| `/punish <player> <category> <reason> <time>` | Applies the punishment immediately, no GUI. | `spunish.punish`, `spunish.punish.<category>` |
| `/unban <player> [reason]` | Revokes an active ban. | `spunish.unpunish.ban` |
| `/unmute <player> [reason]` | Revokes an active mute. | `spunish.unpunish.mute` |
| `/record <category>` | Opens the staff-wide audit report for a category. | `spunish.record`, `spunish.record.others` |
| `/record <category> <staffer>` | Opens the report restricted to one staffer's punishments. | `spunish.record` (own), `spunish.record.others` (anyone else) |
| `/history <player>` | Opens (or, from console, prints) the target's punishment history. | `spunish.history` |
| `/spunishreload` | Reloads `reasons.yml` and `messages.yml`. | `spunish.admin.reload` |

## Permissions

See [docs/permissions.md](docs/permissions.md) for the full node table and example staff group setups.

## Configuration

s-punish ships sensible defaults for all four config files (`config.yml`, `reasons.yml`, `messages.yml`, `gui.yml`), created automatically on first boot. A minimal `config.yml` database section looks like:

```yaml
database:
  host: localhost
  port: 3306
  database: spunish
  user: spunish
  password: "changeme"
  table-prefix: "sp_"
  use-ssl: false

server:
  id: "backend-1"
```

See [docs/configuration.md](docs/configuration.md) for every key across all four files, and how the reload command's atomicity works.

## Documentation

- [docs/configuration.md](docs/configuration.md) — every config key.
- [docs/permissions.md](docs/permissions.md) — the full permission node table.
- [docs/database.md](docs/database.md) — schema, indexes, retention.
- [docs/deployment.md](docs/deployment.md) — deployment order, pool sizing, fail modes.
- [docs/testing.md](docs/testing.md) — the manual platform test matrix.
- [docs/troubleshooting.md](docs/troubleshooting.md) — common failure modes.
- [CONTRIBUTING.md](CONTRIBUTING.md) — branching, commit conventions, running the tests.

## License

[MIT](LICENSE).
