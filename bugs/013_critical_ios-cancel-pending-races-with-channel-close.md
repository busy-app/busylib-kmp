# Pending iOS GATT writes/reads can hang on disconnect because `cancelPending` is asynchronous

## Type
infrastructure

**Severity:** critical

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheralGattIO.kt` (lines 190-203)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheral.kt` (lines 128-149, `onDisconnect`)

## Summary

`FPeripheralGattIO.cancelPending(...)` posts the cancellation work via
`launchWithLock(writeDeferredMutex, scope, "cancel_pending_write")` and
`launchWithLock(readDeferredMutex, scope, "cancel_pending_read")`. Both
return immediately, before the deferreds are actually completed.

`FPeripheral.onDisconnect()` then proceeds to:

1. `_rxDataChannel.close()` / `_streamingDataChannel.close()`.
2. Update characteristic-state flows / clear maps.
3. Emit `FPeripheralState.DISCONNECTED`.

Two failure modes follow:

### A. `scope` already cancelled → cancellation lambdas never run

`cancelPending` is invoked from `onDisconnect`, which is itself called from
`FCentralManager.didDisconnect` / `updateBLEStatus`. If the consuming feature
scope (the one that owns `FPeripheral`) has already been cancelled by the
SDK shutdown path, the two `launchWithLock` jobs get cancelled *before*
acquiring the mutex, so the entries in `writeDeferreds` / `readDeferreds`
remain. The waiting `withTimeout(WRITE_ACK_TIMEOUT_MS) { deferred.await() }`
in `writeSerial` / `readValue` / `writeValue` then sleeps the full
`WRITE_ACK_TIMEOUT_MS = 10s` and finally throws
`TimeoutCancellationException` rather than the intended
`CancellationException("Disconnected")`.

### B. Race against `waitConnected()` re-entry

After `_stateStream.emit(FPeripheralState.DISCONNECTED)` is observed, future
calls to `writeSerial`/`readValue`/`writeValue` suspend inside
`waitConnected()` (filtering on `CONNECTED`). That is fine. But if the
device reconnects (state goes back to `CONNECTED`) before the still-pending
`launchWithLock` cancellation lambdas run, those lambdas now wipe **fresh**
entries in `writeDeferreds`/`readDeferreds` belonging to the new
session, completing them with the stale `disconnectException`. The new
caller spuriously fails its first I/O with `CancellationException`.

## Reproduction

A. iOS user toggles BT off while a `bleHttpEngine.execute(...)` is mid-write.
The consumer cancels the connection scope. Observe that the suspended writer
sleeps for 10 s before completing with `TimeoutCancellationException`
instead of cancelling immediately.

B. Device disconnects (BLE drop) and immediately reconnects within ~1 ms;
the first `writeValue` on the reconnected session fails with
`CancellationException("Disconnected")`.

## Root cause

`cancelPending` should perform its work synchronously from the calling
coroutine (which is `onDisconnect` — already a `suspend fun`). Using
`launchWithLock(scope, …)` decouples the cancellation from the lifecycle
event that requires it.

## Impact

- 10-second hangs on every pending GATT request when an iOS app is
  backgrounded / BT-toggled (instead of immediate disconnect propagation).
- Cross-session leak: pending deferred completion can affect the next
  connection of the same `FPeripheral` instance after a flap.

## Suggested fix

```kotlin
suspend fun cancelPending(disconnectException: CancellationException) {
    withLock(writeDeferredMutex, "cancel_pending_write") {
        writeDeferreds.values.forEach { it.completeExceptionally(disconnectException) }
        writeDeferreds.clear()
    }
    withLock(readDeferredMutex, "cancel_pending_read") {
        readDeferreds.values.forEach { it.completeExceptionally(disconnectException) }
        readDeferreds.clear()
    }
}
```

and `await` it from `onDisconnect` *before* changing state / closing
channels. This guarantees waiters are released before the disconnect
finishes, and never resurrects on a future session.
