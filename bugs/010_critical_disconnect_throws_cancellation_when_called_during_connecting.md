# `FDeviceHolder.disconnect()` throws `CancellationException` when called while `deviceApi` is still connecting

## Severity
critical

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:90-99`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:129-145` (caller)

## Summary
`FDeviceHolder.disconnect()` first does `deviceApi.cancelAndJoin()`, then immediately calls
`runSuspendCatching { deviceApi.getCompleted() }`. If the `async { ... connect ... }` had not yet
completed, `cancelAndJoin` puts the `Deferred` into the *cancelled* state. Calling `getCompleted()`
on a cancelled (not normally completed) `Deferred` throws a `CancellationException`. The project's
`runSuspendCatching` is explicitly designed to *re-throw* `CancellationException` (see
`ResultKtx.kt:34-35`). The exception therefore propagates out of `disconnect()`, out of
`disconnectInternalUnsafe`, out of `withLock`, and into the caller of `connectIfNot` /
`disconnectCurrent`, which suddenly looks like its own coroutine was cancelled.

## Reproduction / scenario
1. Caller invokes `connectIfNot(configA)`. Orchestrator builds a `FDeviceHolder`, the `async` is
   running but `deviceConnectionHelper.connect(...)` is still suspending (BLE scan, TCP handshake,
   Cloud websocket negotiation – the normal multi-second case).
2. Caller invokes `connectIfNot(configB)` (a different device) or `disconnectCurrent()`.
3. Orchestrator runs `disconnectInternalUnsafe()` which calls `currentDeviceLocal.disconnect()`.
4. Inside `disconnect()`: `deviceApi.cancelAndJoin()` cancels the still-running `async`.
5. `runSuspendCatching { deviceApi.getCompleted() }` fires `CancellationException`.
6. `runSuspendCatching` rethrows the cancellation.
7. The exception unwinds through `withLock` (mutex is released) and crashes the *caller's*
   coroutine.

## Why it happens
- `Deferred.cancelAndJoin()` puts a non-completed Deferred into the cancelled state.
- `Deferred.getCompleted()` only returns normally if the Deferred completed *successfully*; for a
  cancelled Deferred it throws the cancellation cause (a `CancellationException`).
- The project's `runSuspendCatching` deliberately re-throws `CancellationException` to preserve
  cooperative cancellation semantics.
- Therefore the surrounding `runSuspendCatching` does not "swallow" the case the author seems to
  expect (i.e. "ignore if not completed normally").

## Impact
- `disconnectCurrent()` (used by FConnectionService.forgetDevice and similar "user logs out" flows)
  cancels the caller's coroutine instead of returning normally.
- A `connectIfNot(configB)` performed while configA is still in `Connecting` state cancels the
  caller's coroutine before the new connection can begin – the user sees no transition at all,
  and depending on the call site a higher-level scope may be killed.
- The orchestrator's mutex is correctly released (withLock unlocks on exception), but
  `currentDevice` is left non-null because line 144 (`currentDevice = null`) is never reached.
  The next `connectIfNot` will see a stale `currentDevice` referring to a holder whose scope was
  partially torn down – the holder's `deviceApi` is cancelled, its scope is *not* yet cancelled
  (line 97-98 was never reached), and its listener may still emit on the old transport listener.

## Suggested fix
Don't use `getCompleted()` after a `cancelAndJoin`. Snapshot the result before cancelling:

```kotlin
suspend fun disconnect() {
    info { "Find active device api, start disconnect" }
    val api = if (deviceApi.isCompleted && !deviceApi.isCancelled) {
        runSuspendCatching { deviceApi.await() }.getOrNull()
    } else null
    deviceApi.cancel()
    api?.disconnect()
    scope.cancel()
    scope.coroutineContext.job.join()
}
```

or use `deviceApi.getCompletionExceptionOrNull()` to discriminate cancelled vs. completed before
calling `getCompleted()`.
