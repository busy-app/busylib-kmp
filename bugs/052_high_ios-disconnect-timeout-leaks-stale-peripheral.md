# Disconnect timeout in `FCentralManager.disconnect` leaves the stale peripheral entry in `_connectedStream`

## Type
infrastructure

**Severity:** high

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/central/FCentralManager.kt` (lines 120-145)

## Summary

```kotlin
override suspend fun disconnect(id: NSUUID) {
    …
    val peripheral = _connectedStream.first()[id] ?: return
    peripheral.onDisconnecting()
    manager.cancelPeripheralConnection(cbPeripheral)

    withTimeoutOrNull(BleConstants.DISCONNECT_TIME) {
        peripheral.stateStream.first {
            it == FPeripheralState.DISCONNECTED || …
        }
    } ?: run {
        warn { "Disconnect timeout for peripheral id=$id, forcing cleanup" }
        peripheral.onDisconnect()                      // ← updates state only
    }
    // ← `_connectedStream` is NOT mutated here. Removal happens in
    //   `didDisconnect()` which never fired (timeout).
}
```

If CoreBluetooth fails to deliver `peripheral:didDisconnectPeripheral:`
within 10 seconds (radio glitch, OS scheduling, suspended app), the
fallback path:

1. Forces `peripheral.onDisconnect()` so `stateStream` reaches
   `DISCONNECTED`, but
2. Leaves the entry in `_connectedStream` with state `DISCONNECTED`.

The next `BLEDeviceConnectionApiImpl.waitForPeripheralConnect` (which is
the only post-disconnect re-entry path) reads:

```kotlin
val existingDevice = centralManager.connectedStream.value[deviceIdentifier]
if (existingDevice != null) {
    existingDevice.stateStream
        .filter { it == FPeripheralState.DISCONNECTED }
        .first()                       // returns immediately
}
```

so it appears to "succeed" but never disposes the old peripheral, resulting
in a leaked `FPeripheral`, leaked `_rxDataChannel`/`_streamingDataChannel`
(closed only via `onDisconnect`, which has already run, so OK) — and more
importantly, a leaked `CBPeripheral.delegate` reference that is **not**
overwritten until a brand-new `FPeripheral` is created for a fresh
identifier. Any late `didUpdateValue` / `didWriteValue` on the underlying
CBPeripheral runs on the leaked `FPeripheralDelegate`, calling into a
disposed `FPeripheralValueRouter` (whose channels are closed → `runBlocking
{ channel.send(...) }` throws, the exception is swallowed because it is in
a `runBlocking` from the BLE callback queue).

## Reproduction

1. Disconnect a device, simulate `peripheral:didDisconnectPeripheral:` not
   being delivered (suspend the app, kill BLE, etc.) so the 10-second
   timeout fires.
2. Reconnect to the same device. The old `FPeripheral` is still in
   `_connectedStream` with state `DISCONNECTED`.
3. Trigger any value notification on the old `CBPeripheral` (in tests,
   call `delegate.peripheral(_:didUpdateValueForCharacteristic:error:)`):
   `runBlocking { rxDataChannel.send(...) }` throws
   `ClosedSendChannelException`, which on the iOS main queue propagates
   as an uncaught exception → app crash.

## Root cause

Forced cleanup updates state but does not also remove the entry from
`_connectedStream` and does not detach the `CBPeripheral.delegate`.

## Impact

- Memory leak (one `FPeripheral` per failed disconnect).
- Potential crash on iOS main queue when late callbacks fire.
- Reconnect to the same address re-uses a half-dead `FPeripheral`
  reference for one tick before it is replaced.

## Suggested fix

In the timeout branch:

```kotlin
} ?: run {
    warn { "Disconnect timeout for peripheral id=$id, forcing cleanup" }
    peripheral.onDisconnect()
    _connectedStream.update { it - id }
    // Detach delegate to silence late callbacks:
    cbPeripheral.delegate = null
}
```

(or call a new `FPeripheral.dispose()` that also nulls the delegate).
