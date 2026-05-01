# `FDeviceHolder.disconnect()` redundantly cancels scope after `cancelAndJoin`

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:90-99`

## Summary
```kotlin
suspend fun disconnect() {
    info { "Find active device api, start disconnect" }
    deviceApi.cancelAndJoin()
    runSuspendCatching { deviceApi.getCompleted() }
        .getOrNull()
        ?.disconnect()
    info { "Cancel scope" }
    scope.coroutineContext.job.cancelAndJoin()
    scope.cancel()
}
```

After `scope.coroutineContext.job.cancelAndJoin()` returns, the scope's `Job` is in `Cancelled`
state and `isActive == false`. The subsequent `scope.cancel()` is a no-op.

More problematic: the order of operations does not actually clean up resources properly.

1. `deviceApi.cancelAndJoin()` cancels the `async`. But the `async` was created via
   `scope.async { ... }`, which is a child of the scope's job. Cancelling the child does *not*
   cancel the parent scope's job.
2. After `cancelAndJoin`, the deviceApi may have been completed normally *before* the cancel
   landed, so `getCompleted()` returns the API → we call `api.disconnect()`.
3. Then we cancel the scope → invokeOnCompletion fires → listener called with Disconnected (a
   *second* time, after `api.disconnect()` already triggered transport-level Disconnected).

If `deviceConnectionHelper.connect` started additional coroutines on the same scope (a common
pattern – e.g. a long-running poll for cloud heartbeats), those children are not specifically
cancelled by `deviceApi.cancelAndJoin()`. They *are* cancelled by `scope.coroutineContext.job.
cancelAndJoin()`. So step 3 is doing the actual heavy lifting; step 1's `cancelAndJoin` is only
useful for the case where the deviceApi async is still suspending – in which case it triggers
the bug described in `critical_disconnect_throws_cancellation_when_called_during_connecting.md`.

## Reproduction / scenario
- Read the source. The `scope.cancel()` line cannot do anything useful after
  `scope.coroutineContext.job.cancelAndJoin()`.

## Why it happens
- Defensive-programming reflex: "cancel everything we can think of". But cancelAndJoin already
  did everything cancel() would do.

## Impact
- Pure dead code; minor cosmetic issue. Could mask a future refactor that genuinely needs a
  second cancel (i.e., bug-prone redundancy).

## Suggested fix
Remove the dead line. Also reconsider the ordering – ideally, call `api.disconnect()` first (when
api is available), *then* cancel the scope:

```kotlin
suspend fun disconnect() {
    info { "Disconnect requested" }
    val api = if (deviceApi.isCompleted && !deviceApi.isCancelled) {
        runSuspendCatching { deviceApi.getCompleted() }.getOrNull()
    } else null
    api?.disconnect()
    scope.coroutineContext.job.cancelAndJoin()
}
```
