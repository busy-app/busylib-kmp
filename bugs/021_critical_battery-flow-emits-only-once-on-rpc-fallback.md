# Battery flow emits only once and dies on RPC fallback path

## Severity
critical

## Type
broken-feature

## Files
- `components/bridge/feature/battery/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/battery/impl/FDeviceBatteryInfoFeatureApiImpl.kt`
  - `getRpcBatteryInfoFlow()` (lines 85–98)
  - `getDeviceBatteryInfo()` (lines 100–111)

## Summary
On transports without a BLE GATT meta channel (LAN/Cloud or when GATT battery characteristic is absent), the public `getDeviceBatteryInfo()` flow performs a single one-shot RPC fetch and then completes forever. If that single RPC call fails, the flow emits **nothing**, leaving consumers stuck. There is no retry, no polling, and no event-driven update.

## Repro
1. Connect to BUSY Bar over a transport where `metaInfoApi` does not yield BATTERY_LEVEL/BATTERY_POWER_STATE (e.g. cloud TCP).
2. Subscribe to `FDeviceBatteryInfoFeatureApi.getDeviceBatteryInfo()`.
3. Either:
   - The first `/api/status/power` request fails → consumer sees nothing forever.
   - The first request succeeds → consumer sees a single value and the flow completes.
4. Battery state changes on the device are never reported.

## Root Cause
```kotlin
private fun getRpcBatteryInfoFlow(): Flow<BSBDeviceBatteryInfo> {
    return flow { emit(rpcFeatureApi.fRpcSystemApi.getStatusPower().getOrNull()) }
        .filterNotNull()  // null on RPC failure → empty flow
        .map { ... }
}
```
- `getOrNull()` swallows failures by emitting `null`, which `filterNotNull()` strips, completing the flow with zero emissions.
- There is no `exponentialRetry`, no periodic re-fetch, and no subscription to power events from `FEventsFeatureApi` (`BusyLibUpdateEvent.Power`), even though the events feature already produces them.
- `flatMapLatest { ... }` then completes too, and the public `WrappedFlow` stays silent forever.

## Impact
- UI / consumers cannot show real-time battery on non-GATT transports.
- After the first emission, charging/discharging transitions and percentage updates are completely lost.
- A single transient HTTP failure permanently disables battery reporting until reconnect.

## Suggested Fix
- Wrap the RPC call in `exponentialRetry { rpcFeatureApi.fRpcSystemApi.getStatusPower() }` so transient failures are retried.
- Subscribe to `FEventsFeatureApi.get<BusyLibUpdateEvent.Power>()` like other features do (settings, wifi) and merge it with the initial RPC fetch via `transformWhileSubscribed { throttleLatest { ... } }`. The `Power` event already carries `batteryChargePercent` and `isCharging`.
- For pure HTTP polling fallback, periodically re-fetch with a `delay`/`while (isActive)` loop similar to `FWiFiFeatureApiImpl.getWifiStateFlow()`.
- Do not silently drop the failure with `getOrNull()`.
