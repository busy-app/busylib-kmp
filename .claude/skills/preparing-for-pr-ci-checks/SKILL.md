---
name: preparing-for-pr-ci-checks
description: Use before every git commit or git push in busylib-kmp to mirror the GitHub PR CI checks (detekt, allTests, publishing) locally so CI does not go red.
---

# Preparing for PR CI Checks

Before any `git commit` or `git push` in this repo, reproduce the checks that run in `.github/workflows/pr.yml` locally. If any of them fail on CI, the PR is blocked — it is cheaper to catch and fix them here.

## CI jobs this skill mirrors

From `.github/workflows/pr.yml`:

| CI job | Local equivalent |
|---|---|
| `gradle_validation` | wrapper files are in the repo — nothing to run, just don't touch `gradle/wrapper/*` |
| `detekt_validation` | `./gradlew detektFormat` (see loop below) |
| `tests_validation` | `./gradlew allTests` |
| `sample_cmp_validation` | `./gradlew :sample:cmp:assemble` (only if you touched `sample/cmp` or shared code used by it) |
| `publish_internal` | `./gradlew publishToMavenLocal` (see publishing section) |

## Lint + test loop (follow in order, do not skip)

1. Run `./gradlew detektFormat`
2. **If it failed**, run the exact same command **once more**. `detektFormat` applies auto-fixes on the first pass; the second run validates that the tree is now clean.
3. **If it failed a second time**, STOP. Ask the user to fix the remaining lint issues manually. Do NOT hand-edit files to silence detekt — the rules encode project invariants (see `detekt-rules/` — `ApiWrappedTypeRule`, `SerialNameNotProvidedRule`, etc.) and patching around them hides real problems.
4. Run `./gradlew allTests`
5. **If any test failed**, fix the test (or the code it exposed), then **go back to step 1**. Fixing code can re-introduce formatting drift, so detekt must pass again before you trust the tests.

Only proceed to commit/push once both `detektFormat` (step 1–3) and `allTests` (step 4) are green.

### Linux-only note

Apple targets cannot be built on Linux. Before running `publishToMavenLocal` on Linux, add this line to `local.properties`:

```
flipper.appleEnabled=false
```

This disables `iosArm64`, `iosSimulatorArm64`, `iosX64`, `macosArm64`, `macosX64` publishing for that machine. Do **not** commit `local.properties` — it is developer-local.

On macOS, leave `flipper.appleEnabled` unset (defaults to enabled) so the full matrix publishes.

## Red flags — STOP before committing

- `./gradlew detektFormat` still failing after two consecutive runs
- `./gradlew allTests` failing (even a single test)
- `./gradlew publishToMavenLocal` failing

If any of these are red, do **not** create the commit or push. Surface the failure to the user and ask how to proceed.

## Common mistakes

| Mistake | Fix |
|---|---|
| Running `allTests` before `detektFormat` passes | Lint failures often point at real bugs; tests can give a false sense of safety. Always lint first. |
| Hand-editing code to silence a detekt rule | Rules in `detekt-rules/` enforce API invariants. Silencing = hiding bugs. Ask the user instead. |
| Running `publishToMavenLocal` on Linux without `flipper.appleEnabled=false` | Build fails on Apple targets. Set the flag in `local.properties` first. |
| Skipping the second `detektFormat` run | The first run rewrites files; the second confirms the tree is stable. Both are required. |
| Committing with tests failing "because they're flaky" | The repo's tests are gating. Fix the test, don't bypass it. |
