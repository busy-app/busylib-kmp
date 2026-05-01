# medium — `tryUpdateConnectionConfig` host-change check is fragile

## Severity
medium

## Type
broken-feature

## Files
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FLanApiImpl.kt:57-69`
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FCloudApiImpl.kt:54-66`

## Summary
```kotlin
if (currentConfig.copy(name = config.name) == config) {
    currentConfig = config
    return Result.success(Unit)
}
return Result.failure(IllegalArgumentException("Config $config has different non-name fields"))
```

This pattern abuses `data class` equality to detect "only the name field differs". It works **today** (LAN: host+name; Cloud: deviceId+name), but adding a new field to either config silently breaks the check — the new field will be compared with `==` and any change in it (even an innocuous tag) returns failure where success may be desired, OR vice versa.

Also: `currentConfig` is mutated in place (`var currentConfig`). If `tryUpdateConnectionConfig` is called concurrently with another `tryUpdateConnectionConfig` (rare but possible) we get torn reads — `currentConfig` is read multiple times in the function. Should be guarded by a Mutex.

`BUSYBarHttpEngine` was constructed with `currentConfig.host` and never updated; if a future config change actually CAN change the host (e.g. mDNS rediscovery → new IP), the HTTP engine still points at the old host. The current "fail on non-name change" behavior masks this latent bug, but does so silently.

## Repro
- Add a third field to `FLanDeviceConnectionConfig` (e.g. `port`).
- Existing logic now requires that field to be unchanged AND name unchanged for "success" — the spec is ambiguous.

## Root Cause
Data-class identity used as a partial-comparison primitive.

## Impact
- Silent breakage on schema change.
- Unfixable IP migration on LAN if device IP changes.
- Concurrent call hazard.

## Suggested Fix
```kotlin
override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): CResult<Unit> = mutex.withLock {
    if (config !is FLanDeviceConnectionConfig) return@withLock CResult.failure(...)
    if (currentConfig.host != config.host) {
        return@withLock CResult.failure(IllegalArgumentException("Host change requires reconnect"))
    }
    currentConfig = config
    CResult.success(Unit)
}
```

Be explicit about which fields are mutable in place.
