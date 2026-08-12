# Deployment

## Runtime coordinates (verified 2026-08-11)

`design.md`'s premise — that "Paper 26.2" is the Paper API for Minecraft
1.26.2 — was confirmed. No toolchain downgrade was necessary.

| Item | Value |
| --- | --- |
| Paper API | `io.papermc.paper:paper-api:26.2.build.112-stable` |
| Velocity API | `com.velocitypowered:velocity-api:4.0.0` |
| Repository for both artifacts | `https://repo.papermc.io/repository/maven-public/` |
| Java required by Paper 26.2 | 25 |
| Java required by Velocity (this setup) | 25 |
| Build tool | Gradle 9.7.0 |

The project's toolchain is pinned to Java 25 in the root `build.gradle.kts`
convention, with no downgrade planned — both runtimes support it natively.

`velocity-api` is pinned to the stable `4.0.0` release rather than the
in-development `4.1.0-SNAPSHOT` — Velocity's own compatibility documentation
confirms support up to Minecraft 1.26.2 already on the stable line, and a
build dependency should never point at a SNAPSHOT.

## Deployment order

1. Provision MySQL 8.0+ and the application user; confirm it's reachable
   from the proxy and from every backend.
2. Start **one** backend with the plugin. Migrations create the schema on
   first boot.
3. Validate issuance, expiration, history and reporting on that single
   backend before rolling out further.
4. Roll out to the remaining backends with the same `config.yml`, changing
   only `server.id` on each.
5. Install the Velocity module last — backends already block login on
   their own, so the proxy module is reinforcement, not a prerequisite.
6. Deny the vanilla punishment commands (`minecraft.command.ban`,
   `minecraft.command.pardon`, `minecraft.command.ban-ip`, etc.) to staff,
   so s-punish is the only path to punish someone.

## Proxy connection pool sizing

Velocity only ever reads an active ban at login and consumes sync events —
a small pool is enough even for a large network. The `config.yml` the
Velocity module creates on first boot (a bundled default distinct from each
backend's own `config.yml`) already ships with `maximum-pool-size: 4` and
`minimum-idle: 1`; don't raise those unless the proxy is serving an unusually
large number of backends at once.

## Fail modes

`fail-mode.login` and `fail-mode.chat` in `config.yml` each independently
control what happens when storage is unreachable:

- **`login`** (default `DENY`) — the ban check at login. `DENY` refuses
  login with the configurable storage-unavailable message during an outage,
  so a banned player can never slip in while the database is down. `ALLOW`
  lets everyone in, banned or not, until storage recovers.
- **`chat`** (default `ALLOW`) — the mute-state lookup that seeds a
  player's chat cache at login. `ALLOW` seeds "not muted"; `DENY` denies the
  login outright instead, since there's no representable "blocked pending
  resolution" state in the chat cache.

Both apply on every platform that performs the corresponding check (Paper
backends for both; the Velocity proxy for `login` only, since it never
touches chat). See [troubleshooting.md](troubleshooting.md) for what an
outage looks like from the affected commands' point of view.

## Manual edge verification (proxy absent)

Requires real infrastructure (a Paper backend with the plugin, and MySQL)
and was therefore not executed in the sandbox this project was built in —
documented here for whoever validates a real deployment (see also
`docs/testing.md`, which covers the same manual matrix more broadly):

1. Apply a test ban directly on the backend
   (`/punish <player> ban <reason> <time>`).
2. With no Velocity module installed on any proxy, connect directly to the
   backend's port (bypassing any proxy) as the banned player.
3. Confirm the login is refused with the configurable ban screen — i.e.
   that the backend's own enforcement is sufficient by itself, and the
   Velocity module is edge reinforcement, not a prerequisite (consistent
   with installing it last in the deployment order above).
