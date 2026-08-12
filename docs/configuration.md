# Configuration

SPunish reads four YAML files from its data folder (`plugins/SPunish/` on a
Paper backend, `plugins/spunish/` on the Velocity proxy). Each is created
from a bundled default the first time it's missing, and is loaded with
[Configurate](https://github.com/SpongePowered/Configurate) into a typed,
validated object — a file that fails to parse or validate never partially
replaces what was already in effect.

The Velocity proxy only reads `config.yml` and `messages.yml` (with a
smaller default connection pool — see [deployment.md](deployment.md)). It has
no reason catalog and no GUIs, so `reasons.yml` and `gui.yml` don't apply to
it.

## Atomic reload

`/spunishreload` (`spunish.admin.reload`) reloads exactly two files:
`reasons.yml` and `messages.yml`. Each is parsed and validated into a fresh
object before anything currently in effect is touched; if either fails, the
command reports every error back to the caller and the previous, still-valid
catalog/messages stay in effect untouched. `config.yml` and `gui.yml` are
read once at boot and require a restart to change.

## `config.yml`

| Key | Meaning |
| --- | --- |
| `database.host`, `.port`, `.database`, `.user`, `.password` | MySQL connection details. |
| `database.table-prefix` | Prepended to all four table names, so multiple installs can share one schema. |
| `database.use-ssl` | Whether the JDBC connection uses SSL. |
| `database.pool.maximum-pool-size` / `.minimum-idle` / `.connection-timeout-ms` / `.max-lifetime-ms` | HikariCP pool sizing. |
| `database.query-timeout-ms` | Timeout applied to every async storage call. |
| `server.id` | This instance's stable identifier, recorded as `origin_server` on every punishment it applies and used to ignore its own sync events. Blank derives a fallback from the hostname and logs a warning — set it explicitly for a stable identity across restarts. |
| `sync.poll-interval-ms` | How often this instance polls `sync_events` for changes made elsewhere. |
| `sync.overlap-ms` | How far back each poll re-checks, to close the `AUTO_INCREMENT` commit-order race (see [database.md](database.md)). |
| `sync.event-retention-ms` | How long consumed sync events are kept before being pruned. |
| `fail-mode.login` | `DENY` or `ALLOW` when storage is unreachable during the ban check at login. Defaults to `DENY` — a banned player must not slip in during an outage. |
| `fail-mode.chat` | Same, for the mute-lookup that seeds the chat cache at login. Defaults to `ALLOW`. |
| `retention.enabled` | Whether closed (revoked or expired) punishments older than `retention-days` are periodically purged. Disabled by default. Active punishments are never purged, regardless of age. |
| `retention.retention-days` | The retention window, in days. |
| `mute.blocked-commands` | Command labels (without the leading `/`) blocked while muted, aliases included. |
| `mute.warning-cooldown-ms` | Minimum time between repeated "you are muted" warnings for the same player. |
| `report.cooldown-ms` | Minimum time between fresh report aggregations requested by the same user. |
| `report.cache-duration-ms` | How long a computed report is served from cache before a new aggregation runs. |
| `report.ranking-limit` | Maximum staff ranking entries returned by a general report. |
| `duration-limits` | Maps a permission suffix (after `spunish.limit.`) to the maximum duration it grants, e.g. `trial: 7d` for `spunish.limit.trial`. A staffer matching no entry has no restriction; matching more than one gets the most permissive. |
| `suggested-durations` | Extra `<time>` values offered on top of a reason's own default when tab-completing `/punish`. |

## `reasons.yml`

A list per category (`ban`, `mute`) of:

| Key | Meaning |
| --- | --- |
| `id` | Unique within its own category — the same id may exist under both `ban` and `mute`. |
| `display-name` | Shown to the target and staff, and frozen onto the punishment record at the moment it's applied — renaming a reason later never changes existing history. |
| `duration` | `n` for permanent, or a `quantity+unit` sequence (`s`, `m`, `h`, `d`, `w`, `mo`, `y`), e.g. `30d` or `1h30m`. Used as the default when no explicit `<time>` is given (the GUI path always uses this). |
| `icon` | A `Material` name for the GUI item. |
| `description` | Free text, shown nowhere yet but kept for staff-facing documentation of the reason. |
| `permission` | Optional. If set, only staffers holding it can see or use this reason. |

## `messages.yml`

Every value under `messages:` may be a single line, a list of lines (sent in
order), or blank/empty to turn that message off entirely. All of them are
[MiniMessage](https://docs.advntr.dev/minimessage/format.html); a malformed
tag degrades to plain text and logs the offending key once. `{token}`
placeholders are substituted per message; an unknown one is left literal and
logged once. `{prefix}` is available everywhere.

Top-level keys outside `messages:` configure formatting rather than text
content: `prefix`, `date-time.pattern`/`.zone` (also used for report window
calendar boundaries), `duration.*` (unit labels, max units shown, permanent
text), `state.*` (colored active/expired/revoked labels), and
`report.window.*` (window names). Everything under `messages.*` is grouped by
feature (`general`, `punish`, `unpunish`, `reload`, `screen`, `history`,
`record`, `gui.*`) matching the command or GUI that renders it.

## `gui.yml`

Structural only — sizes, slots, and icon `Material`s for the category,
reason, history, and report menus. All text (titles, item names, lore) comes
from `messages.yml`'s `gui.*` keys instead, resolved when each menu is built.
