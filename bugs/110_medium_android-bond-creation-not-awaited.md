# Android `connectUnsafe` calls `device.createBond()` without awaiting completion

## Type
infrastructure

**Severity:** medium

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/androidMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/BLEDeviceConnectionApiImpl.kt` (lines 85-91)

## Summary

```kotlin
if (!device.hasBondInformation) {
    device.createBond()
    info { "Create bond with device" }
}

info { "Request the highest mtu" }
device.requestHighestValueLength()
```

`createBond()` is asynchronous on Android: the bond machine pops a system
dialog, the user accepts, and the result arrives via the bond-state
listener. The current implementation logs and proceeds straight into
`requestHighestValueLength()` and onwards into service discovery /
notify-subscription — all of which require an authenticated link if any
characteristic uses encryption.

If the user takes longer than a few hundred ms to accept the pairing
prompt, the subsequent GATT calls fail with
`GATT_INSUFFICIENT_AUTHENTICATION` / `GATT_INSUFFICIENT_ENCRYPTION`,
which surface as silent characteristic-discovery failures (see also
`medium_android-services-stateflow-runs-eagerly-on-caller-scope.md` for
why discovery failures are silent).

## Reproduction

1. Wipe pairing for the device on the phone settings.
2. Initiate connect.
3. Delay tapping "Pair" by ~3 seconds.
4. Observe RX/TX char enumeration fails. Connection may *appear* to
   succeed, but every subsequent write hangs.

## Root cause

`device.createBond()` returns a `Boolean`/`Unit` (depending on the wrapper
library), it does not suspend until bonding completes. The Nordic
`no.nordicsemi.kotlin.ble.client.android` API exposes
`peripheral.bondState` as a `Flow<BondState>`; we should await
`BondState.BONDED` before requesting MTU / discovering services.

## Impact

- Race between OS pairing dialog and GATT operations.
- Connection silently fails when bonding takes longer than expected.

## Suggested fix

```kotlin
if (!device.hasBondInformation) {
    device.createBond()
    withTimeout(BleConstants.CONNECT_TIME) {
        device.bondState.first { it == BondState.BONDED }
    }
    info { "Bonded with device" }
}
```

Add an explicit error if bond ends in `NONE` (user denied).
