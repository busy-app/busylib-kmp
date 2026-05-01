# `FSettingsFeatureApi.getDeviceName()` initial value is the connection-time name, not live

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/feature/settings/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/settings/api/FSettingsFeatureApiImpl.kt` (lines 53–75, 149–151)

## Summary
`deviceNameFlow` is built as a `StateFlow` whose initial value is `connectedDevice.deviceName`. `connectedDevice` here is the `FConnectedDeviceApi` snapshot captured at feature creation; its `deviceName` is whatever the transport reported at *connect* time (advertised name, mDNS hostname, etc.). It is **not** kept in sync with the device's current name. When the user renames the device via `setDeviceName(...)` we re-emit the new name, but:

- Late subscribers that connect, immediately collect, see a stale advertised name as the initial state, then a brief pause until the upstream `getName(couldConsume)` lands, then the real name. That flicker is observable.
- If `FRpcSettingsApi.getName` keeps failing forever (`exponentialRetry { ... }` with `Long.MAX_VALUE` retries), the stale value lingers indefinitely.
- After a re-connect to the same physical device, a *new* `FConnectedDeviceApi` is captured and its `deviceName` may again be stale (because the BLE adv name lags the on-device name change by a long time).

## Repro
1. Rename device to "Office BUSY Bar" via the device.
2. Reconnect from the lib (BLE advertising still says "BUSY-12345").
3. Subscribe to `getDeviceName()` — first emission is `"BUSY-12345"`, then eventually `"Office BUSY Bar"`.

## Root Cause
- `stateIn(scope, SharingStarted.Lazily, connectedDevice.deviceName)` uses the transport's display name as the seed.
- The transport's `deviceName` is not refreshed across the lifetime of the device API.

## Impact
- UI shows a wrong name for the duration of one RPC round-trip after every connection (or forever if the RPC fails).
- Internal logic that gates on `getDeviceName().value` (e.g. tab titles, scope keys) is incorrect during this window.

## Suggested Fix
- Initialise the StateFlow with `null` (or a `Loading` sentinel) and switch to `WrappedStateFlow<String?>` so the consumer can distinguish "not yet loaded" from a value.
- Or fetch the canonical name eagerly in the factory and seed the StateFlow with it.
- Cap retries; surface failure via a sentinel state instead of looping forever.
