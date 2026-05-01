# `SetupFinishedBeforeKrate` is a global singleton — leaks completion state across devices

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/feature/finish-setup/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/finishsetup/krate/SetupFinishedBeforeKrate.kt`
- `components/bridge/feature/finish-setup/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/finishsetup/api/FFinishSetupFeatureApiImpl.kt` (lines 197–203)

## Summary
`SetupFinishedBeforeKrate` is bound `@ContributesBinding(BusyLibGraph::class, ...)` and stored under a single `Settings` key `"setup_was_finished_before"`. The "finish setup" feature writes `true` once the first BUSY Bar has all setup tasks completed. After the user pairs a *different* BUSY Bar (or factory-resets the existing one), the krate still reports `true`, so `taskListResourceFlow` immediately yields `FFinishSetupState.FinishedBefore` and the user never sees the setup flow.

## Repro
1. Connect BUSY Bar #1, walk through finish-setup until krate is saved as `true`.
2. Disconnect; connect BUSY Bar #2 (or factory-reset #1, then reconnect).
3. Subscribe to `FFinishSetupFeatureApi.taskListResourceFlow` → first emission is `FinishedBefore`, regardless of #2's actual setup state (BLE not paired, WiFi not configured, account not linked, etc).

## Root Cause
- `SetupFinishedBeforeKrateImpl` uses one fixed key, no device discriminator.
- `transformWhileSubscribed { ... if (isSetupFinishedBefore) return@throttleLatest FFinishSetupState.FinishedBefore }` short-circuits all other checks.

## Impact
- New BUSY Bars cannot be onboarded normally.
- A factory-reset BUSY Bar appears "already setup" to the host app.
- Multi-device users lose access to the finish-setup UI on every device after the first.

## Suggested Fix
- Key the krate by device-stable identifier, ideally serial number from `BusyBarStatusDevice.serialNumber`. The factory should resolve the serial via `FAboutFeatureApi`/`FRpcSystemApi` and inject a per-device wrapper, e.g. `Settings.getBoolean("setup_was_finished_before:$serial", false)`.
- Alternatively expose a "reset finished-before" method tied to disconnect/factory-reset events.
- Add a test that verifies switching device identity flips the krate back to `false`.
