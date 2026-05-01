# iOS peripheral never reaches `CONNECTED` if the device has no meta-info characteristics

## Type
infrastructure

**Severity:** critical

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheral.kt` (lines 113-126; `onConnecting`, `onConnect`)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheralValueRouter.kt` (lines 88-95)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/BLEDeviceConnectionApiImpl.kt` (lines 138-160; waits on `FPeripheralState.CONNECTED`)

## Summary

The iOS state machine has no transition into `FPeripheralState.CONNECTED`
from the `onConnect()` / discovery-complete path. The only place
`CONNECTED` is emitted is the side-effect block in
`FPeripheralValueRouter.didUpdateValue`:

```kotlin
if (metaKey != null) {
    stateStream.emit(FPeripheralState.CONNECTED)
    updateMetaInfo(key = metaKey, data = payload)
}
```

Consequently:

- `FBleDeviceConnectionConfig.metaInfoGattMap.isEmpty()` ⇒ no meta key ever
  matches ⇒ peripheral is stuck in `CONNECTING` forever and
  `BLEDeviceConnectionApiImpl.waitForPeripheralConnect` times out at 30s
  even though the radio is actually connected and the serial GATT is ready.
- A device whose meta-info notify characteristic happens to fire after
  `BleConstants.CONNECT_TIME` (slow boot, large advertising interval) hits
  the same timeout for a working device.

## Reproduction

1. Build a `FBleDeviceConnectionConfig` with `metaInfoGattMap =
   persistentMapOf()` (or simply targeting a peripheral whose meta GATT
   service is missing/optional).
2. Observe successful TCP/BLE connection at the iOS layer (CB peripheral
   `state == 2`), services and characteristics discovered, RX notify enabled.
3. After 30 s, `waitForPeripheralConnect` times out and `connect` returns
   `Result.failure(NoFoundDeviceException())`, masking the fact that the
   connection actually succeeded.

## Root cause

`onConnect()` performs `peripheral.discoverServices(null)` but never
schedules a `_stateStream.emit(CONNECTED)`. The author of
`FPeripheralValueRouter` reused that emit as an "all good" signal, but it
is not specified by the state machine and depends on optional behaviour.

## Impact

- Silent connection failure for any device without meta-info GATT.
- Connection attempts on slower devices fail at 30 s even though the radio
  is fine.
- Users see "device not found" errors that retrying does not fix.

## Suggested fix

Move the transition to `CONNECTED` to a deterministic point — for example,
when both serial RX/TX and (if configured) streaming-notify
characteristics have been discovered and notify-enabled. Concretely:

- `FPeripheralDiscovery.didDiscoverCharacteristics` already knows when the
  serial service is "ready" — emit `CONNECTED` from there (via a callback
  to `FPeripheral`) once readiness is reached.
- Remove the `stateStream.emit(CONNECTED)` from
  `FPeripheralValueRouter.didUpdateValue` (also removes the resurrection
  bug captured in `critical_ios-state-resurrection-from-meta-update.md`).
- Ensure `metaInfoGattMap.isEmpty()` is a fully supported configuration in
  unit tests.
