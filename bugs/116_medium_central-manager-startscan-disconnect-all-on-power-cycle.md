# `FCentralManager.updateBLEStatus` blanket-disconnects on every non-`POWERED_ON` state, even transient ones

## Type
infrastructure

**Severity:** medium

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/central/FCentralManager.kt` (lines 174-191)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/central/FBLEStatus.kt` (initial value `UNKNOWN`)

## Summary

```kotlin
override val bleStatusStream: WrappedStateFlow<FBLEStatus> =
    _bleStatusStream.asStateFlow().wrap()

private val _bleStatusStream = MutableStateFlow(FBLEStatus.UNKNOWN)
…
private suspend fun updateBLEStatus(state: CBManagerState) {
    val newStatus = FBLEStatus.from(state)
    _bleStatusStream.emit(newStatus)
    if (newStatus != FBLEStatus.POWERED_ON) {
        _discoveredStream.emit(emptySet())
        _connectedStream.first().values.forEach { peripheral ->
            peripheral.onDisconnect()
        }
        _connectedStream.emit(emptyMap())
    }
}
```

Two state values are problematic:

1. **`RESETTING`** is a transient state during a Bluetooth-stack reset on
   iOS that lasts a few hundred ms. It is followed by `POWERED_ON` if BT
   is enabled. The current logic eagerly tears down every existing
   peripheral, forcing the consumer to do a full reconnect dance instead
   of waiting for the reset to complete and treating the connection as
   continuing.

2. **`UNKNOWN`** is the initial state of `CBCentralManager` until the
   first delegate update arrives. If the SDK is constructed eagerly and
   delegate registration happens before the iOS BT stack is ready, the
   first state event might be `UNKNOWN`. Because the initial value of
   `_bleStatusStream` is also `UNKNOWN`, `MutableStateFlow.emit` may
   coalesce (no-op), but `updateBLEStatus` is a synchronous mutator, not
   an emit-only path: the disconnect-all logic runs unconditionally,
   even though there are no connections — wasted work but also a
   surprising side-effect.

The bigger issue is that `RESETTING` should not destroy connection state.
CoreBluetooth itself preserves peripherals across a reset.

## Reproduction

1. Connect to a peripheral.
2. On iOS, toggle Bluetooth off then on quickly, or reset the Bluetooth
   stack.
3. Observe `bleStatusStream` go `POWERED_ON → RESETTING → POWERED_ON`. The
   peripheral entry is destroyed during `RESETTING` and the consumer must
   reconnect from scratch even though CoreBluetooth would have automatically
   re-established the link.

## Root cause

The disconnect-everything logic conflates "BT power off" and "BT
reinitialising".

## Impact

- Unnecessary reconnect dances on every short BT stack hiccup.
- Worse user experience on iOS than necessary.

## Suggested fix

```kotlin
if (newStatus == FBLEStatus.POWERED_OFF ||
    newStatus == FBLEStatus.UNAUTHORIZED ||
    newStatus == FBLEStatus.UNSUPPORTED) {
    // Real teardown
    …
}
// RESETTING / UNKNOWN: keep peripherals, just emit status
```
