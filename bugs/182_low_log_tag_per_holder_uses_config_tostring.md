# `FDeviceHolder.TAG = "FDeviceHolder-$config"` uses verbose `toString` of config

## Severity
low

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:57`

## Summary
`override val TAG = "FDeviceHolder-$config"` interpolates the entire `FDeviceConnectionConfig`
into the log tag. `FDeviceConnectionConfig` is an abstract class without a defined `toString` –
implementations may have very long `toString` outputs (e.g., serialized auth tokens, hex BLE
MACs, or whole config classes). Logging frameworks often have tag length limits (Android logcat
truncates at 23 characters on older API levels; many platforms truncate at some bound).

Beyond truncation, sensitive data could leak into logs. If the config holds a Cloud auth token,
or a username, that data ends up in the log tag of every emission related to this holder.

## Reproduction / scenario
- Connect to a device with a Cloud config that contains a session token. Every log line tagged
  with `TAG` exposes the token.

## Why it happens
- Convenience interpolation of an unconstrained type into the tag.

## Impact
- Possible PII / secret leakage into logs.
- Possible truncated/unreadable log tags on some platforms.

## Suggested fix
Use `uniqueId` (which is already a parameter) – it's a stable, identifiable, non-sensitive
value:

```kotlin
override val TAG = "FDeviceHolder-$uniqueId"
```
