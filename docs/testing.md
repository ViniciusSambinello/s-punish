# Manual platform test matrix

Everything testable was pushed down into `spunish-common`, which has full
automated coverage (JUnit 5 for domain/service logic, Testcontainers/MySQL
for storage, reporting, sync and migrations). The platform modules
(`spunish-paper`, `spunish-velocity`) are reduced to adaptation and
presentation — real event handlers, real GUIs, real network sockets — which
is exactly the part that can't run in a unit test. This matrix is the
checklist for verifying that adaptation layer against real servers.

None of this has been executed against real infrastructure yet (see
`docs/deployment.md`'s manual edge verification note and tasks.md's 10.6 and
11.9) — run it before a first real release, and whenever platform-layer code
changes.

## Setup

- At least two Paper 26.2 backends and one Velocity 4.0.0 proxy, all pointed
  at the same MySQL database with distinct `server.id`s.
- A staff account with every `spunish.*` permission, and a plain account
  with none.

## Paper: issuance and revocation

- [ ] `/punish <online-player>` as a player opens the category GUI, showing
      only categories the staff account can apply.
- [ ] `/punish <online-player> ban` opens the reason GUI for `BAN` directly.
- [ ] Clicking a reason applies the punishment with its default duration,
      closes the GUI, and the author gets the confirmation message.
- [ ] `/punish <offline-known-player> ban <reason> <time>` applies correctly
      against an offline target; it takes effect on their next login.
- [ ] `/punish <player> ban <unknown-reason> <time>` is refused with the
      reason-not-found message.
- [ ] The console can run the full four-argument form but is refused (with
      the "use the full form" message) on the one- and two-argument forms.
- [ ] A banned online player is kicked immediately, with the configurable
      ban screen.
- [ ] `/unban <player>` revokes an active ban; the player can log back in.
- [ ] `/unmute <player> <reason>` revokes an active mute with a reason; the
      revoked reason shows up in `/history`.
- [ ] A staffer without `spunish.punish.override` gets "already active" when
      punishing an already-punished target in the same category; one with
      override supersedes it, and both records appear in history.

## Paper: enforcement

- [ ] A muted player's chat messages are blocked, with the configurable
      mute-warning message shown, rate-limited by `mute.warning-cooldown-ms`.
- [ ] Every command listed in `mute.blocked-commands` (and its registered
      aliases) is blocked while muted.
- [ ] A banned player attempting to connect directly to a backend (bypassing
      the proxy) is refused with the ban screen.

## Paper: history and reporting

- [ ] `/history <player>` opens a paginated GUI, newest first, with a
      working category filter that resets to page one.
- [ ] An entry's remaining time, permanent text, and (once revoked) revoker/
      reason are all correct; clicking it shows the punishment id and origin
      server.
- [ ] Console `/history <player>` prints the same data as text.
- [ ] `/record ban` shows the staff-wide totals, state breakdown, staff
      ranking, and reason distribution for the daily/weekly/monthly/all-time
      windows, switching without closing the GUI.
- [ ] `/record ban <staffer>` restricted to one staffer works for your own
      name without `spunish.record.others`, and is refused for anyone else's
      name without it.
- [ ] Requesting a report again within `report.cooldown-ms` shows the
      cooldown message or serves the cached result, without a fresh query.

## Paper: config reload

- [ ] `/spunishreload` after editing `reasons.yml`/`messages.yml` applies
      the change immediately, with no restart.
- [ ] `/spunishreload` after introducing an invalid `reasons.yml` (e.g. a
      duplicate id) reports the validation error and leaves the previous
      catalog in effect.

## Velocity: proxy edge

- [ ] A banned player connecting through the proxy is refused at login,
      before ever reaching a backend, with the same configurable ban screen
      a backend would show.
- [ ] Banning a player already connected through the proxy (on any backend)
      disconnects them from the network within the sync propagation window
      (`sync.poll-interval-ms` plus a couple of seconds).
- [ ] With the Velocity module removed entirely, a banned player connecting
      directly to a backend's port is still refused — the backend's own
      enforcement doesn't depend on the proxy module being present.

## Fail-mode behavior

- [ ] Stop MySQL, then attempt to log into a backend: with
      `fail-mode.login: DENY` (default), login is refused with the
      storage-unavailable message; with `ALLOW`, login succeeds.
- [ ] With MySQL stopped, attempt to chat right after logging in (before the
      cache would have a real answer): with `fail-mode.chat: DENY`, the login
      itself is refused; with `ALLOW` (default), the player logs in unmuted.
- [ ] Restore MySQL and confirm normal enforcement resumes without a
      restart.
