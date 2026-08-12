# Contributing

## Branches

- `main` — always releasable. Protected: requires a green CI run and a pull
  request, no direct pushes.
- `develop` — integration branch. Feature/fix work branches off it and merges
  back into it by pull request (squash merge).
- `feat/*`, `fix/*`, `chore/*` — one branch per unit of work, named after
  what it does.

Releases are cut from a pull request merging `develop` into `main`, tagged
`vX.Y.Z` from `main` afterward.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/), enforced in CI
by commitlint. Format:

```
<type>(<scope>): <description>
```

Scope is optional, but when given must be one of `common`, `paper`,
`velocity`, `docs`, `build` — use the module a change actually touches;
`docs` and `build` cover documentation and Gradle/CI-only changes
respectively. Common types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`.

Examples:

```
feat(paper): add punishment commands and tab complete
fix(common): correct revocation rate calculation for an empty window
docs(paper): document the manual platform test matrix
```

## Running the tests

```bash
./gradlew build          # compile + unit tests, every module
./gradlew integrationTest # Testcontainers-backed tests — requires Docker
```

Unit tests (`./gradlew test`) never require Docker or a real database and
run as part of `build`. Integration tests (`./gradlew integrationTest`, on
`spunish-common` and `spunish-paper`) spin up real MySQL containers via
Testcontainers and are kept out of `build`/`check` on purpose, so a plain dev
machine without Docker isn't forced to have it. CI runs both.

## Code style

- Java 25. Records for data, sealed interfaces with exhaustive pattern
  matching for closed sets of states/results.
- Comments state a business rule, not what the next line of code obviously
  does — if a comment would just restate the code, delete the comment
  instead.
- `spunish-common` never references a Bukkit/Paper/Velocity class. Anything
  platform-specific goes through one of the five ports in
  `com.spunish.common.platform` (`PermissionChecker`, `AudienceResolver`,
  `PlayerKicker`, `MainThreadDispatcher`, `ServerIdentity`), implemented per
  platform.
- No synchronous storage or player resolution on a platform's main
  thread — resolve first, then hop back for the platform-specific side
  effect (message, GUI, kick).

## Pull requests

Target `develop` unless you're specifically preparing a release (`main`).
Keep a PR scoped to one logical change — several small PRs are easier to
review than one that touches every module.
