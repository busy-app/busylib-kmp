# `BusyLibUpdateEvent.Power` collapses CHARGED into "not charging"

## Severity
medium

## Type
broken-feature

## Files
- `components/bridge/feature/events/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/events/proto/protomapper/delegates/PowerProtobufMapper.kt` (lines 10–14)
- `components/bridge/feature/events/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/events/model/BusyLibUpdateEvent.kt` (lines 19–22)
- `components/bridge/feature/battery/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/battery/model/BSBDeviceBatteryInfo.kt`

## Summary
`BusyLibUpdateEvent.Power` is modelled as `(batteryChargePercent, isCharging)` — a Boolean. The protobuf mapper sets `isCharging = (battery_status == BatteryStatus.CHARGING)`. The firmware-level `BatteryStatus` distinguishes at least DISCHARGING/CHARGING/CHARGED (mirrored in the RPC `PowerState` enum), but the event throws away CHARGED (becomes `isCharging = false`, indistinguishable from DISCHARGING).

Meanwhile `BSBDeviceBatteryInfo.BSBBatteryState` does carry CHARGED/CHARGING/DISCHARGING. So the public battery model can express CHARGED, but the live event flow cannot. As soon as a feature flips from RPC-derived state to event-derived state, the CHARGED state is lost (becomes DISCHARGING in derived models).

## Repro
1. Plug the BUSY Bar into a charger and let the battery hit 100% (CHARGED).
2. Observe a Power protobuf event arrive; `BusyLibUpdateEvent.Power.isCharging` is `false`.
3. Any consumer that converts `Power` → `BSBDeviceBatteryInfo` will produce `state = DISCHARGING` even though the device is plugged in and charged.

## Root Cause
- `Power` model uses `Boolean` instead of an enum.
- `PowerProtobufMapper.map` reduces multi-state to bool.

## Impact
- "Plugged in, fully charged" is reported as "discharging" once event-driven state takes over.
- Charging UI affordances (lightning bolt, percentage colour) flicker at 100%.

## Suggested Fix
- Replace `isCharging: Boolean` with `state: BSBDeviceBatteryInfo.BSBBatteryState` in `BusyLibUpdateEvent.Power`.
- Update `PowerProtobufMapper` to map all three (and `Unrecognized` to a sensible default).
- Note: changing this is a public-API change, but the field is consumed only inside the lib (battery feature does not currently subscribe to events).
