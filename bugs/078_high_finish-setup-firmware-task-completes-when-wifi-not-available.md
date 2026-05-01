# Firmware-update setup task is reported COMPLETED when WiFi status is unknown

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/feature/finish-setup/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/finishsetup/api/FFinishSetupFeatureApiImpl.kt`
  - `createUpdateFirmwareTask()` (lines 106–129)

## Summary
When the WiFi connectWifi task is `LOADING` or `NOT_AVAILABLE` (e.g. before status is known, or wifi is in an indeterminate state), `createUpdateFirmwareTask` reports the firmware task as `COMPLETED`. This is logically wrong: we have no idea whether an update is needed and the task should be `LOADING` (or `NOT_AVAILABLE`), not `COMPLETED`. As a side effect, when this triggers `tasks.all { it.status == COMPLETED }` to be true, the code flips `setupFinishedBeforeKrate.save(true)` and the entire setup flow transitions to `FFinishSetupState.FinishedBefore` permanently (the krate is persisted across launches).

## Repro
1. Connect to a fresh BUSY Bar.
2. WiFi status arrives as `UNKNOWN` initially → `connectWifiTask.status = NOT_AVAILABLE`.
3. `createPairBleTask` is `null` (e.g. on JVM/macOS where `fBleFeatureApi == null`).
4. `createLinkAccountTask` returns `NOT_AVAILABLE` (reasonable; depends on wifi).
5. `createUpdateFirmwareTask` returns `COMPLETED` (the bug).
6. With `pairBleTask = null`, `connectWifi = NOT_AVAILABLE`, `linkAccount = NOT_AVAILABLE`, `updateFirmware = COMPLETED` — `tasks.all { COMPLETED }` is `false` because of `NOT_AVAILABLE`. But on a slightly different state (e.g. wifi `CONNECTED`, link `SameUser`, no firmware update available) this incorrect classification can flip the krate true prematurely.
7. More importantly, even if it never flips the krate, the user sees "Update firmware" task ticked off while the app has no actual information about firmware status.

## Root Cause
```kotlin
status = when (connectWifiTaskStatus) {
    DeviceSetupTaskStatus.COMPLETED -> { ... real check ... }
    DeviceSetupTaskStatus.NOT_COMPLETED -> DeviceSetupTaskStatus.NOT_AVAILABLE
    DeviceSetupTaskStatus.LOADING,
    DeviceSetupTaskStatus.NOT_AVAILABLE -> DeviceSetupTaskStatus.COMPLETED   // <-- wrong
}
```
`LOADING` and `NOT_AVAILABLE` mean "we don't know yet" / "not applicable" — they should map to `LOADING` or `NOT_AVAILABLE`, never `COMPLETED`.

## Impact
- Firmware update task is reported done even when the lib has no firmware status.
- Combined with the persisted `SetupFinishedBeforeKrate` (singleton across devices/launches), there is a path that latches the user permanently into "FinishedBefore" state without ever having checked for an update.
- Also, `SetupFinishedBeforeKrate` is a singleton not keyed by device serial → connecting a new BUSY Bar after another finished setup before is reported as already finished.

## Suggested Fix
- In `createUpdateFirmwareTask`, change the fallback branch:
  ```kotlin
  DeviceSetupTaskStatus.LOADING -> DeviceSetupTaskStatus.LOADING
  DeviceSetupTaskStatus.NOT_AVAILABLE -> DeviceSetupTaskStatus.NOT_AVAILABLE
  ```
- Key `SetupFinishedBeforeKrate` by device serial number (e.g. via `BusyBarStatusDevice.serialNumber`) to avoid cross-device false positives.
- Add unit tests asserting that `tasks.all { COMPLETED }` is never true while any task is `LOADING`/`NOT_AVAILABLE`.
