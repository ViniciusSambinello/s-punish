# Troubleshooting

## The plugin fails to enable: "Could not connect to the database"

The connection pool fails fast at construction — if MySQL is unreachable,
wrong credentials, or the wrong host/port, the plugin refuses to enable
rather than start half-working. Check `config.yml`'s `database.*` section
and confirm the server can reach MySQL on that host/port. The error message
and stack trace never include the password, even on failure — safe to paste
into a bug report.

## The plugin fails to enable: "Database schema version N is newer than the M this plugin version supports"

The database was migrated by a newer version of the plugin than the one
currently running — for example, a backend was rolled back after another
backend already applied a migration ahead of it. Update this binary to a
version that supports schema version N, or restore the newer binary on every
backend. There is no automatic downgrade path; migrations never remove data,
so nothing is lost by fixing the version mismatch and restarting.

## A punishment or revocation takes a few seconds to show up on another server

This is expected, not a bug. Instances converge by polling `sync_events`
every `sync.poll-interval-ms` (default 2s) plus a small processing margin —
there's no push notification between servers. If propagation is taking
longer than a few seconds beyond that interval:

- Confirm every backend's clock and `sync.poll-interval-ms` are reasonable;
  a very large interval directly adds to the delay.
- Confirm `server.id` is set explicitly and differs on every instance — two
  instances sharing an id will each ignore the other's events, since an
  instance always ignores events it thinks it originated.
- Check the log for "Sync event consumption failed" warnings — a persistent
  failure means the affected instance keeps retrying the same window rather
  than losing events, but enforcement on that instance won't reflect
  network-wide changes until it's resolved.

The instance that *applied* the change always reflects it immediately on
itself — only *other* instances go through the polling path.

## A message shows up unformatted / with visible tags like `<red>`

A malformed MiniMessage tag in `messages.yml` makes that specific message
key degrade to plain, unformatted text rather than crash or produce garbled
output — check the server log for a "Malformed MiniMessage tag in message
'<key>'" warning naming the exact key and file. Fix the tag (a common cause
is an unclosed tag, e.g. `<red>text` without `</red>`) and either restart or
run `/spunishreload` if the broken key was in `reasons.yml`'s reach — for
`messages.yml` itself, `/spunishreload` re-parses it and will show the same
warning again if the fix didn't take.

## A reloaded `reasons.yml`/`messages.yml` didn't seem to take effect

`/spunishreload` validates the new file in full before replacing anything
currently in effect — if it reports errors, the previous, valid
configuration is still the one in effect, on purpose. Fix every reported
error and reload again.
