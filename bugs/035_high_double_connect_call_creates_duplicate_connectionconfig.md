# `connectIfNot` builds the connection config twice and may use stale config for the holder

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:71-91`

## Summary
`connectIfNot` calls `deviceConnectionConfigMapper.getConnectionConfig(config)` *twice* per
invocation – once on line 71 (used for `tryToUpdateConnectionConfig`), and again on line 91 (passed
to `deviceHolderFactory.build`). If the mapper has any side effect, time-dependent output, or
relies on mutable state (e.g. the latest known LAN IP address, a freshly-issued auth token, a list
of bonded BLE peripherals at scan-time), the holder ends up using a different config than the one
that was just deemed unable to be live-updated. The two configs can disagree, leading to a holder
that actually contradicts the user's most recent intent.

## Reproduction / scenario
1. `getConnectionConfig` builds config snapshot at T0 (BLE address X, LAN address Y).
2. `tryToUpdateConnectionConfig` is invoked with snapshot at T0 and fails (e.g., async still
   pending).
3. Some other coroutine updates the underlying state between T0 and T1.
4. Line 91 builds *new* snapshot at T1 (BLE address X', LAN address Y') and wires the holder with
   that.
5. The orchestrator logs a "Failed to update current connect" message that referred to T0, then
   builds a holder with T1 – the user's logical action ("connect to T0 config") was discarded.

## Why it happens
`getConnectionConfig` is invoked with the same input but is called twice instead of being
cached locally. There is no obvious reason for the second call.

## Impact
- Subtle wrong-config bugs: "the device sometimes connects with stale parameters when I tap
  reconnect quickly".
- Wasted work on the mapper.
- Logs become misleading because the failure reason references a config the holder isn't actually
  built with.

## Suggested fix
Compute the config once and reuse it:

```kotlin
val connectionConfig = deviceConnectionConfigMapper.getConnectionConfig(config)
// ... 
currentDevice = deviceHolderFactory.build(
    uniqueId = config.uniqueId,
    config = connectionConfig,         // <-- reuse the same snapshot
    listener = { ... },
    ...
)
```
