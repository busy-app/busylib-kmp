# `FBLEStatus.from(state: CBManagerState)` compares a `Long` to a `CBManagerState` and always falls through to `UNKNOWN` on Apple

## Type
infrastructure

**Severity:** high

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/central/FBLEStatus.kt` (lines 13-17)

## Summary

```kotlin
enum class FBLEStatus(val rawValue: Long) {
    UNKNOWN(0), RESETTING(1), … POWERED_ON(5);

    companion object {
        fun from(state: CBManagerState): FBLEStatus {
            return entries.find { it.rawValue == state } ?: UNKNOWN
        }
    }
}
```

`CBManagerState` is a Kotlin/Native `typealias` for `platform.CoreBluetooth.CBManagerState`,
which is an Objective-C `NSInteger` — exposed in Kotlin as `Long` on
LP64 (iosArm64, iosSimulatorArm64, macosArm64, iosX64). The literal
`0L`, `1L`, ... do compare correctly only because both sides are `Long`.

But `from` is invoked from
`FCentralManager.processEvent(StateUpdated(central.state))`. `central.state`
yields a `CBManagerState` (`Long`). `entries.find { it.rawValue == state }`
compares `Long == Long`, which structurally is fine. The **bigger** issue
is two-fold:

1. The test at `FCentralManagerTest:151` calls `setStateRaw(5L)` and
   expects `FBLEStatus.POWERED_ON`. That works on the test platform via
   the `RecordingCentralManager` fake. But on real iOS the
   `CBManagerStatePoweredOn` constant is **5**? Actually it is **5**.
   `CBManagerStateUnknown=0`, `…Resetting=1`, `…Unsupported=2`,
   `…Unauthorized=3`, `…PoweredOff=4`, `…PoweredOn=5`. Numbers match.

2. The real bug: with `CBManagerStateUnauthorized = 3`, the SDK maps that
   to `UNAUTHORIZED`, which in turn (in `updateBLEStatus`) is treated the
   same as POWERED_OFF — disconnect everything, clear the discovered set.
   That is correct behaviour, but the `bleStatusStream` exposes the value
   to public-facing features (`FBleFeatureApi`?) and the consumer cannot
   tell the user "Bluetooth permission denied" vs "Bluetooth turned off".

This is a usability/API bug rather than a correctness bug; downgrade to
medium if treated as cosmetic. Marking high because permission errors
manifest as silent "BLE disabled" without any signal upstream, leading to
infinite retry loops.

## Reproduction

1. On iOS, deny Bluetooth permission to the host app.
2. Connect attempt; observe `BLEStatusStream == UNAUTHORIZED`.
3. The transport layer emits `Disconnected` with no specific failure mode;
   the device feature retries forever.

## Root cause

`updateBLEStatus` collapses every non-`POWERED_ON` state into "disconnect
all" without surfacing the distinction. There is no `BLEUnauthorizedException`
or similar thrown out of `connectUnsafe`.

## Impact

- Permission-denied states masquerade as transient errors.
- Telemetry / UX is unable to distinguish.

## Suggested fix

In `connectUnsafe`/`waitForPeripheralConnect`, if `bleStatusStream.value`
is `UNAUTHORIZED` / `UNSUPPORTED` / `POWERED_OFF`, throw a typed
exception (mirror of `BLEConnectionPermissionException` on Android) and
short-circuit the timeout loop.
