# `FDeviceHolder` scope is leaked when `deviceApi` `async` fails before `disconnect()` is called

## Severity
critical

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:64-78`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:101-119`

## Summary
The holder's `scope` is created with a `CoroutineExceptionHandler`, *not* a `SupervisorJob`. The
`deviceApi` is started as `scope.async { ... }`. When the `async` fails:

- The exception is *contained* inside the Deferred and surfaced via `await/getCompleted`.
- Because the parent of the `async` is the scope's regular `Job` (no `SupervisorJob`), and
  exceptions from `async` are *not* propagated to the parent until somebody calls
  `await`/`getCompleted` and rethrows, the scope's job is **not** cancelled by the failure.
- `onConnectError` is called via the chained `.onFailure { ... }.getOrThrow()`. `getOrThrow()`
  rethrows the failure inside the `async`, which then completes the Deferred exceptionally. But
  the *scope's job* still does not become cancelled, because nothing awaits the deferred.
- `invokeOnCompletion` on the *scope's* job therefore never fires (the scope is still alive).
- The holder is left in a zombie state: `deviceApi.isCompleted = true && isCancelled = false &&
  exceptionOrNull = the failure`. The scope is alive. Any other coroutines launched inside the
  scope are also alive.

The orchestrator does eventually clean up: `onConnectError` triggers `onInternalDisconnect`,
which schedules `globalScope.launch { withLock { disconnectInternalUnsafe(holder) } }`. That
calls `currentDeviceLocal.disconnect()` → which calls `deviceApi.cancelAndJoin()` (no-op on a
completed Deferred) → `runSuspendCatching { deviceApi.getCompleted() }` → throws the original
exception (NOT a `CancellationException`, so `runSuspendCatching` *does* swallow it). Returns
null. Then `scope.coroutineContext.job.cancelAndJoin()` cancels the scope. OK – cleanup happens.

So the leak window is: from when the async fails until the orchestrator's queued globalScope
cleanup runs and acquires the mutex. During that window, any other children of the scope keep
running.

The *real* issue arises if `deviceConnectionHelper.connect` started long-running children on
the holder's scope, *and* the orchestrator's cleanup is delayed by the mutex (e.g., a long
in-flight `connectIfNot` for a different device is still running). Those children continue to
run on the dispatcher, possibly hammering the (already-failed) transport, possibly emitting
spurious status updates that go straight to the orchestrator's listener, racing with the
cleanup. None of these emissions are filtered out before the holder is fully cleaned up.

## Reproduction / scenario
1. `connectIfNot(deviceA)` succeeds at building the holder, the `async` starts the connection.
2. Inside `connect()`, the implementation launches a child coroutine on `scope` for "transport
   keepalive" or "metadata refresh".
3. `connect()` itself fails with IOException.
4. The orchestrator queues `globalScope.launch { withLock { ... } }` for cleanup.
5. The user holds the orchestrator mutex via a long-running `connectIfNot(deviceB)` (e.g., slow
   mapper).
6. Window: 100ms-1s. During this window, the keepalive coroutine on holderA's scope keeps
   running. It may emit `Connecting` / `Connected` status updates via the listener.
7. The orchestrator's listener receives these stale updates and forwards them to
   `localTransportListener_A`, which is no longer the active listener but still updates its
   internal MutableStateFlow.
8. Eventually the orchestrator's cleanup fires and cancels the scope.

## Why it happens
- No SupervisorJob: `async` exceptions are silent.
- No proactive cleanup of the scope from inside the holder when the deviceApi fails.

## Impact
- Window of time where the failed holder's child coroutines keep running.
- Stale status updates emitted during that window.
- If the keepalive does any expensive work (RPC retries, network probes), it wastes resources.

## Suggested fix
- Use `SupervisorJob` if you want children to fail independently, **and** explicitly handle
  `deviceApi` exceptions to cancel the scope:

```kotlin
private val deviceApi: Deferred<API> = scope.async {
    try {
        deviceConnectionHelper.connect(scope, config, transportConnectionListener).getOrThrow()
    } catch (e: Throwable) {
        if (e !is CancellationException) {
            onConnectError(this@FDeviceHolder, e)
        }
        scope.cancel(CancellationException("Connect failed", e))
        throw e
    }
}
```

- Or invoke `scope.cancel()` from the failure path so `invokeOnCompletion` runs and propagates
  the disconnect.
