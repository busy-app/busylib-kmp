# `tryCheckLinkedInfo` issues a redundant RPC and emits the linked status twice on success

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/feature/link/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/link/check/onready/api/FLinkInfoOnReadyFeatureApiImpl.kt` (lines 90–114)

## Summary
After successful auth, `tryCheckLinkedInfo` performs `invalidateLinkedUser` *twice* via `exponentialRetry` and emits to `_status` twice. The first block already emits via `onSuccess { _status.emit(linkedAccountInfo) }`, then immediately afterwards the code does `_status.emit(awaitLinkedAccountInfo(principal))`, which calls `invalidateLinkedUser` *again*. Even on the happy path this is a redundant network round-trip that doubles the time-to-final-state and creates a window where stale state is observed.

## Repro
- Trace `FRpcCriticalFeatureApi.invalidateLinkedUser` calls during the post-`authBusyBar` flow.
- Two calls observed: one inside the first `exponentialRetry` block (lines 105–110) and one inside `awaitLinkedAccountInfo(principal)` (line 111).
- `_status` shared state receives two emissions back-to-back — collectors using `distinctUntilChanged` will only see one, but the upstream cost (HTTP RPC, BLE bytes) is doubled.

```kotlin
exponentialRetry {
    rpcFeatureApi.invalidateLinkedUser(principal.userId)
        .map { result -> result.asSealed(principal.userId) }
        .onSuccess { linkedAccountInfo -> _status.emit(linkedAccountInfo) }   // emit #1 + RPC #1
        .map { }
}
_status.emit(awaitLinkedAccountInfo(principal))                               // emit #2 + RPC #2
```

## Root Cause
- Likely a copy/refactor leftover. The first block performs the work but does not return; the second block re-issues the same RPC and emits again.

## Impact
- Each successful link path triggers an extra RPC against the device — over BLE this is non-trivial (latency, bytes, throttler tokens used).
- Collectors that observe state count (e.g. tests, telemetry) double-count the same transition.
- If the second RPC returns an older/stale answer (rare but possible on flapping cloud-relay paths), the user briefly sees a regressed state.

## Suggested Fix
- Drop one of the two blocks. Either keep the first (use the value already emitted via `onSuccess`) or replace both with a single `_status.emit(awaitLinkedAccountInfo(principal))`.
- Consolidate into a single helper `private suspend fun refreshStatus(principal: BUSYLibUserPrincipal.Token?) { _status.emit(awaitLinkedAccountInfo(principal)) }` and call it once.
- Add a unit test counting `invalidateLinkedUser` invocations to prevent regressions.
