## What does this change?

A short description of what changed and why.

## Related issue

Closes #

## Checklist

- [ ] Commit messages follow [Conventional Commits](../CONTRIBUTING.md#commits)
      with an accepted scope (`common`, `paper`, `velocity`, `docs`, `build`).
- [ ] `./gradlew build` passes locally.
- [ ] `./gradlew integrationTest` passes locally, if this touches storage,
      reporting, sync, or migrations (requires Docker).
- [ ] New behavior has test coverage, or the reason it doesn't is explained
      below.
- [ ] Documentation (`README.md`, `docs/*`) is updated if this changes
      configuration, commands, or permissions.

## Notes for reviewers

Anything that needs context to review well — design tradeoffs, things you're
unsure about, manual testing you did that automated tests can't cover.
