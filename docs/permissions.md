# Permissions

Pure `spunish.*` permission nodes, compatible with any permissions plugin
(LuckPerms, PermissionsEx, or vanilla `op`/groups). No dependency on a
specific permissions manager.

## Nodes

| Node | Grants |
| --- | --- |
| `spunish.punish` | Using `/punish` at all (any of its three forms). |
| `spunish.punish.<ban\|mute>` | Applying that specific category. |
| `spunish.punish.self` | Punishing yourself. Without it, self-punishment is refused. |
| `spunish.punish.override` | Overriding your own exemption checks against a target, and replacing a target's already-active punishment in the same category (the previous one is auto-revoked, attributed to the system). |
| `spunish.reason.<category>.<reason-id>` | Using a reason marked `permission:` in `reasons.yml`. Reasons without a `permission:` key need nothing extra. |
| `spunish.exempt.<ban\|mute>` | Being protected from that category — punishing an exempt target requires the author to hold `spunish.punish.override`. |
| `spunish.unpunish.<ban\|mute>` | Revoking (`/unban`/`/unmute`) an active punishment in that category. Applying a category never implicitly grants revoking it. |
| `spunish.record` | Opening your own report via `/record <category>` (with your own name as staffer) or `/record <category> <your-name>`. |
| `spunish.record.others` | Opening the staff-wide general report, or any other staffer's individual report. Required in addition to `spunish.record`. |
| `spunish.history` | Opening `/history <player>` for any player. |
| `spunish.notify` | Receiving the staff announcement on every punishment and revocation, network-wide. |
| `spunish.admin.reload` | Running `/spunishreload`. |
| `spunish.limit.<key>` | Caps the maximum duration a staffer may apply, including the permanent case, to whatever `duration-limits.<key>` resolves to in `config.yml`. Holding none of these nodes means no restriction; holding more than one applies the most permissive. |

## Example staff groups

A **trial moderator** — can mute, can't ban, capped at 7 days by
`duration-limits.trial: 7d` in `config.yml`:

```
spunish.punish
spunish.punish.mute
spunish.limit.trial
spunish.history
spunish.record
```

A **moderator** — can ban and mute, sees their own report and history, no
duration cap, no override:

```
spunish.punish
spunish.punish.ban
spunish.punish.mute
spunish.unpunish.ban
spunish.unpunish.mute
spunish.history
spunish.record
spunish.notify
```

An **admin** — everything, including overriding active punishments and
seeing anyone's report:

```
spunish.punish
spunish.punish.ban
spunish.punish.mute
spunish.punish.self
spunish.punish.override
spunish.unpunish.ban
spunish.unpunish.mute
spunish.history
spunish.record
spunish.record.others
spunish.notify
spunish.admin.reload
```

A **protected staffer** (e.g. a streamer who shouldn't be muted by mistake by
a trial mod, but can still be handled by someone with override):

```
spunish.exempt.mute
```
