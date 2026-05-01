# `forgetDevice` uses `Result.onFailure` (Kotlin std `Result`) on cloud REST helpers, but the chain still violates SKIE export contract

## Type
infrastructure

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/service/api/src/commonMain/kotlin/net/flipper/bridge/connection/service/api/FConnectionService.kt` lines 9-16
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/service/impl/src/commonMain/kotlin/net/flipper/bridge/connection/service/impl/FConnectionServiceImpl.kt` lines 106-150

## Summary

`FConnectionService.forgetDevice` returns the enum `ForgetDeviceResult`
directly — that part is fine for SKIE export. But two API-contract notes
from AGENTS.md are relevant:

1. **`barsApi.unlinkBusyBar(...)` returns Kotlin `Result<...>`** — visible
   here only because the impl uses `.toCResult()` to reshape it into the
   library's wrapper type before checking `isFailure`. This pattern is
   used inside the suspend `forgetDevice` body, but the wrapper is local
   so SKIE never sees it. Acceptable.
2. **Service hard-codes `BUSYLibUserPrincipal.Loading` filtering with
   `first()`.** If the principal flow never settles past `Loading`
   (e.g. token-refresh hang), `forgetDevice` never returns. Callers in
   Swift/iOS receive an unbounded suspend with no timeout. AGENTS.md
   mandates `exponentialRetry { }` rather than hand-rolled flow waits;
   here, the call is not retry but it is still an unbounded wait.

```kotlin
val principal = principalApi.getPrincipalFlow()
    .filter { principal -> principal !is BUSYLibUserPrincipal.Loading }
    .first() as? BUSYLibUserPrincipal.Token
```

## Repro

1. Principal flow stuck at `Loading` (token refresh hung due to network).
2. User taps Forget.
3. `forgetDevice` enters `transactionInternal` (acquires storage mutex —
   see `medium_forget-device-suspends-without-singletonScope.md`), then
   calls `.first()` on a flow that never emits.
4. `forgetDevice` suspends forever holding the storage mutex.

## Root Cause

`first()` without timeout is unsafe inside a long-held lock. Pair-bug with
`medium_forget-device-suspends-without-singletonScope.md`.

## Impact

- Forget operation may hang indefinitely on a flaky principal refresh.
- Coupled with the storage-mutex hold, this can wedge the whole library.

## Suggested Fix

1. Introduce a bounded wait (e.g. `withTimeoutOrNull(5.seconds)`) for the
   principal resolution.
2. If timed out, return a new
   `ForgetDeviceResult.NOT_AUTHORIZED` (or a new `PRINCIPAL_TIMEOUT`
   variant) rather than holding the mutex.
3. Ideally do principal resolution *before* entering the storage
   transaction.
