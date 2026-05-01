# iOS peripheral state can resurrect to `CONNECTED` after disconnect via late meta-info update

## Type
infrastructure

**Severity:** critical

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheralValueRouter.kt` (lines 88-95)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheral.kt` (`onDisconnect`, `onError`, lines 128-199)

## Summary

`FPeripheralValueRouter.didUpdateValue` unconditionally pushes the state
machine back to `CONNECTED` whenever a known meta-info characteristic value
arrives:

```kotlin
if (metaKey != null) {
    stateStream.emit(FPeripheralState.CONNECTED)
    updateMetaInfo(key = metaKey, data = payload)
}
```

CoreBluetooth is asynchronous; a `didUpdateValueForCharacteristic` event is
permitted to arrive after the central manager has reported
`peripheral:didDisconnectPeripheral:` (e.g. the value was already in flight
on the system queue when the disconnect happened). The router has no
state-guard; it overrides whatever `onDisconnect`, `onError`
(`PAIRING_FAILED`, `INVALID_PAIRING`) or `onDisconnecting` previously set,
and silently emits `CONNECTED`.

The same path is also taken from a runaway Kotlin/Native cinterop call where
`peripheral.value` is non-null but the peripheral itself is no longer
connected — `_stateStream` flicks to `CONNECTED` for an instant, the consumer
observes it via `bleApi.stateStream` (`FIOSBleApiImpl` maps it 1-to-1 to
`FInternalTransportConnectionStatus.Connected`), and now the SDK believes
there is a usable session. Subsequent `writeValue`/`readValue` immediately
pass `waitConnected()` and submit BLE traffic to a peripheral that is gone
— each request will sit in `writeDeferreds`/`readDeferreds` until
`WRITE_ACK_TIMEOUT_MS` fires.

## Reproduction

1. Connect, get meta info populated, observe `CONNECTED`.
2. Trigger a disconnect (BT toggle, distance, peer reset) such that
   `didDisconnect` is delivered while there is one in-flight
   `didUpdateValueForCharacteristic` for `DEVICE_NAME` / `BATTERY_LEVEL` /
   etc. (Easily reproducible by simulator with `peripheral.value` already
   updated by CoreBluetooth.)
3. Observe `connectedStream`/`stateStream` flicker `CONNECTED → DISCONNECTED →
   CONNECTED → DISCONNECTED`.
4. Any consumer reacting to `Connected` (e.g. an auto-resume reader) will
   start traffic that eventually times out.

## Root cause

`didUpdateValue` does not consult the current state before emitting
`CONNECTED`, and is not idempotent w.r.t. being called after disconnect /
pairing failure. The `onDisconnect` path also does not guard
`stateStream` from later transitions (`PAIRING_FAILED` / `INVALID_PAIRING`
are guarded *inside* `onDisconnect` but the late-arriving meta event runs
**outside** of it).

## Impact

- The transport reports `Connected` while the radio is gone.
- HTTP/streaming features start traffic that will time out after up to 10s,
  surfacing to the user as "BUSY Bar is hanging".
- Auto-reconnect logic upstream may flip-flop forever.

## Suggested fix

1. Read the current state before emitting `CONNECTED`:

   ```kotlin
   if (metaKey != null) {
       if (stateStream.value == FPeripheralState.CONNECTING ||
           stateStream.value == FPeripheralState.CONNECTED
       ) {
           stateStream.emit(FPeripheralState.CONNECTED)
       }
       updateMetaInfo(key = metaKey, data = payload)
   }
   ```

2. Even better, decouple "ready/initialised" state from "received first meta
   value". `onConnect` should emit `CONNECTED` directly (or after service
   discovery completes) rather than waiting for a meta value side-effect.
   Devices that *don't* have a meta-info GATT characteristic in
   `metaInfoGattMap` currently never reach `CONNECTED` at all.
3. Make `onDisconnect` set a one-shot `terminal` flag that
   `FPeripheralValueRouter` and `FPeripheralGattIO` consult before mutating
   state.
