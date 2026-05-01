# `onErrorDuringConnect` reports `Disconnected(ERROR_UNKNOWN)` for `CancellationException`

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FTransportListenerImpl.kt:25-38`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:102-113`

## Summary
When a coroutine in the device holder's scope is *deliberately cancelled* (e.g., the user calls
`disconnectCurrent()` and the holder's `disconnect()` cancels the in-flight `async`), the
underlying `connect()` call throws `CancellationException`, which is propagated by `getOrThrow()`
on line 77 of `FDeviceHolder`. That cancellation reaches the
`onFailure { onConnectError(this@FDeviceHolder, t) }` handler on line 75-76 *before* it is
rethrown.

`onConnectError` calls `onInternalDisconnect(deviceHolder) { localTransportListener.
onErrorDuringConnect(config, error) }`. `onErrorDuringConnect` does not special-case
`CancellationException` and updates the state to `Disconnected(reason = ERROR_UNKNOWN)`.

The observer therefore sees a *genuine* error report ("ERROR_UNKNOWN") for what was actually a
clean user-requested cancellation. UI layers that show error toasts on every ERROR_UNKNOWN will
spam the user with errors when they merely switched devices.

## Reproduction / scenario
1. `connectIfNot(deviceA)` → starts connecting.
2. `connectIfNot(deviceB)` → orchestrator calls `disconnectInternalUnsafe()` →
   `holderA.disconnect()` → `deviceApi.cancelAndJoin()`.
3. Inside `holderA`, the `async` block was suspended in `deviceConnectionHelper.connect(...)`.
   That suspension wakes up with CancellationException.
4. The chained `.onFailure { onConnectError(this, t) }` runs synchronously with `t` being a
   CancellationException.
5. `onConnectError` queues a `globalScope.launch { withLock { postAction() } }` where postAction =
   `localTransportListener.onErrorDuringConnect(config, t /* CancellationException */)`.
6. `onErrorDuringConnect` writes `Disconnected(ERROR_UNKNOWN)` to state.
7. UI shows generic "connection error" message.

## Why it happens
- `Result.onFailure` does not discriminate CancellationException; it fires for any failure.
- `onErrorDuringConnect` does not filter out cancellations.

## Impact
- Spurious error reports on every cancel-and-reconnect operation.
- Conflates "the user changed their mind" with "the device errored out".

## Suggested fix
- In `FDeviceHolder.deviceApi`, separate cancellation from "real" errors:
  ```kotlin
  private val deviceApi: Deferred<API> = scope.async {
      runSuspendCatching {
          deviceConnectionHelper.connect(scope, config, transportConnectionListener).getOrThrow()
      }.fold(
          onSuccess = { it },
          onFailure = { t ->
              onConnectError(this@FDeviceHolder, t) // already filters CancellationException via runSuspendCatching
              throw t
          }
      )
  }
  ```
  Note: `runSuspendCatching` re-throws CancellationException, so this naturally distinguishes
  cancel from real errors.

- Alternatively, in `onErrorDuringConnect`, early-return if `throwable is CancellationException`.
