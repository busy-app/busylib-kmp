# `deleteAndLinkAccount()` returns success before re-link completes

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/feature/link/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/link/check/onready/api/FLinkInfoOnReadyFeatureApiImpl.kt`
  - `deleteAndLinkAccount()` (lines 143–155)
  - `tryCheckLinkedInfo()` (lines 90–114)

## Summary
`deleteAndLinkAccount()` is documented as "Deletes current account from BusyBar and tries to link it to current user". After the delete succeeds, the impl calls `tryCheckLinkedInfo(...)` which fires off a `singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) { ... }` — the relink work runs **asynchronously, in a different coroutine, on a different scope** — and `deleteAndLinkAccount()` immediately returns `CResult.Success(Unit)`. The caller has no way to know whether the relink succeeded, failed, or even started. Any subsequent UI logic that assumes success is wrong.

## Repro
1. Connect to BUSY Bar linked to a different user than the local principal.
2. Call `fLinkedInfoOnDemandFeatureApi.deleteAndLinkAccount()`.
3. Inspect the returned `CResult` — always success regardless of relink outcome.
4. Even if the link RPCs error or `withTimeout(ACCOUNT_PROVIDING_TIMEOUT)` (3 seconds!) elapses, the caller is told the operation succeeded.

## Root Cause
```kotlin
override suspend fun deleteAndLinkAccount(): CResult<Unit> {
    return rpcFeatureApi.deleteAccount()
        .onSuccess {
            tryCheckLinkedInfo(...)   // launches singleJobScope.launch{...} and returns immediately
        }
        .map { }
        .toCResult()
}
```
- `tryCheckLinkedInfo` does not `await` anything; it just schedules a job.
- Because the job uses `SingleJobMode.CANCEL_PREVIOUS`, an in-flight `tryCheckLinkedInfo` from `onReady()` will be cancelled, racing with the new one and possibly leaving the `_status` state inconsistent (it can briefly emit pre-delete state).
- `awaitLinkedAccountInfo` is `exponentialRetry` with `Long.MAX_VALUE` retries. Combined with a 3 s timeout in `authBusyBar`, the relink may abort silently while the caller is told everything is fine.

## Impact
- UX flows that show "Account linked" after `deleteAndLinkAccount()` lie to the user when the linking actually failed.
- The `_status` flow is the only signal of completion and it can transition from `Linked.DifferentUser → ... → Linked.SameUser` over an unbounded amount of time. There is no guarantee it ever resolves.
- Race with `onReady()`: cancelling the in-flight check can leave `_status` un-emitted (no replay value) for new subscribers.
- The `withTimeout(3.seconds)` in `authBusyBar` swallows the link timeout via `runSuspendCatching`; nothing surfaces to callers.

## Suggested Fix
- Make `deleteAndLinkAccount()` actually await the relink:
  - Refactor `tryCheckLinkedInfo` to return a `Job`/`Deferred` (or a suspend function), then `await()` it inside `deleteAndLinkAccount` and surface the result.
  - Translate the eventual `LinkedAccountInfo` into success/failure for the caller (e.g. fail if it ends up `NotLinked` or `DifferentUser`).
- Lengthen / make configurable `ACCOUNT_PROVIDING_TIMEOUT`; 3 s is far too aggressive given an unauthenticated cellular hop to BUSY Cloud.
- Avoid `runSuspendCatching` swallowing the timeout in `authBusyBar`; propagate via `Result`.
- Cover with a unit test that fakes a slow relink and asserts the suspend function actually waits for the result.
