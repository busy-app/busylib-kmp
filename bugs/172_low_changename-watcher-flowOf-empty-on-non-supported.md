# `BUSYLibNameWatcher` and `CloudProvisioningWatcher` use bare `flowOf()` for "skip" branches — easy to misread, surprises in tests

## Type
lack-of-feature

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/changename/src/commonMain/kotlin/net/flipper/bsb/watchers/changename/BUSYLibNameWatcher.kt` lines 41-60
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/CloudProvisioningWatcher.kt` lines 49-66

## Summary

Both watchers use a `combine { ... } -> Flow<Flow<X>>; .flatMapLatest { it }`
pattern with `flowOf()` (zero-arg, no values) for the "skip" branches:

```kotlin
when (featureApi) {
    Retrieving, NotFound, Unsupported -> flowOf()           // empty Flow<X>
    is Supported -> featureApi.featureApi.getDeviceName().map { ... }
}
```

`flowOf()` produces a flow that completes immediately without emitting.
Inside `flatMapLatest` this is benign — the inner flow completes, and the
outer flow waits for the next emission from `combine`. So semantically,
"do nothing for this state" works.

Two minor issues:

1. **Readability / convention.** The codebase elsewhere uses `emptyFlow()`
   for the same purpose (e.g. `CloudFetcher.getBarsFlow` uses `emptyFlow()`).
   `flowOf()` here is functionally `emptyFlow<Nothing>()` but is generally
   read as "a flow of one or more values" — easy to misread as "emit a
   single value" when scanning quickly.
2. **Test surprise.** Calling `.first()` on the outer flow during tests
   would suspend forever in the skip branches because `flatMapLatest`
   never produces an outer value. Diagnosing this requires understanding
   `flatMapLatest` inner-completion semantics. Using `emptyFlow()`
   communicates intent more clearly.

## Repro

Not a runtime bug. Static-analysis / readability issue.

## Root Cause

Stylistic inconsistency between watcher modules. `flowOf()` vs `emptyFlow()`
both work but project convention favours the latter for "no values".

## Impact

- Friction during onboarding / debugging.
- No production impact.

## Suggested Fix

1. Replace `flowOf()` with `emptyFlow()` in both watchers.
2. Add a Detekt rule (project already has custom rules per AGENTS.md) that
   flags zero-arg `flowOf()` and recommends `emptyFlow()`.
