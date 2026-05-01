# All post-download install actions (UNPACK/SHA_VERIFICATION/PREPARE/APPLY) report as "Downloading"

## Type
broken-feature

**Severity:** medium

**Files:**
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/updater/mapper/FwUpdateStatusMapper.kt` (lines 33–60)

## Summary

`FwUpdateStatusMapper.fromInstallAction` collapses every non-`NONE` install action into
`FwUpdateState.Downloading(progress = …)`:

```kotlin
when (updateStatus.install.action) {
    BsbAction.DOWNLOAD,
    BsbAction.SHA_VERIFICATION,
    BsbAction.UNPACK,
    BsbAction.APPLY,
    BsbAction.PREPARE -> FwUpdateState.Downloading(...)
    BsbAction.NONE -> fromCheckStatus(...)
}
```

This means after the device finishes downloading, the public `FwUpdateState` continues to claim
"Downloading" for the entire SHA verification, archive unpack, prepare, and apply phases. The `progress`
value is computed from `updateStatus.install.download.totalBytes`, which is meaningless for the unpack/apply
phases — `totalBytes` may be zero or stale, producing `progress = 0f` and a UI that visibly stalls.

In addition, the progress division in this branch does not `coerceIn(0f, 1f)`, so any stale download counter
(`receivedBytes > totalBytes`) will produce `progress > 1f` to consumers.

## Repro

1. Trigger a firmware install with `BsbUpdateVersion.Default`.
2. After the device-side download completes, the device transitions through `SHA_VERIFICATION → UNPACK →
   PREPARE → APPLY`.
3. `FwUpdateState.Downloading` is reported the entire time, with a `progress` that no longer changes (the
   download bytes counter is frozen post-download).
4. Consumer UI shows a progress bar stuck at 100% labeled "Downloading" until the device reboots.

## Root cause

The mapper folds five distinct firmware-install phases into a single user-facing state because there is no
matching state in `FwUpdateState`. `FwUpdateState.Updating` exists but is reserved for after upload-and-
install, not the device-side install phases.

## Impact

- Misleading user feedback. "Downloading" makes the user think the network failed when really the device is
  busy unpacking/applying.
- `progress > 1f` is reachable on stale counters and trips assertions / coerce-style consumers.

## Suggested fix

- Add `FwUpdateState.Verifying`, `FwUpdateState.Unpacking`, `FwUpdateState.Applying` (or a single
  `FwUpdateState.Installing(phase: Phase)` enum-based state) and map each `BsbAction` distinctly.
- `coerceIn(0f, 1f)` on the progress calculation, matching the discipline already used in
  `FirmwareUploaderState.Uploading.progress` and `FirmwareDownloaderState.Downloading.progress`.
