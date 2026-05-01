# medium — `FDeviceConfigToConnectionImpl` config-to-connection lookup uses `isInstance` + first-match (non-deterministic with sub-typing)

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/connectionbuilder/impl/src/commonMain/kotlin/net/flipper/bridge/connection/connectionbuilder/impl/FDeviceConfigToConnectionImpl.kt:41-48`

## Summary
The config-to-connection map is keyed by `KClass<*>`, but the lookup is done with `isInstance` and `find`:
```kotlin
val connectionApiUntyped = configToConnectionMap.entries
    .find { (qualifier, _) -> qualifier.isInstance(config) }
    ?: throw NotImplementedError("Can't find connection for config $config")
```
Two problems:

1. **Non-deterministic match on sub-typing.** `KClass.isInstance(config)` returns `true` for the registered class **and any superclass it inherits from**. Today, every concrete config (`FBleDeviceConnectionConfig`, `FCloudDeviceConnectionConfig`, `FLanDeviceConnectionConfig`, `FMockDeviceConnectionConfig`) is a flat subclass of `FDeviceConnectionConfig`, so there is at most one match. As soon as someone introduces an intermediate base — e.g. `FTcpDeviceConnectionConfig` shared between LAN and Cloud and also registered (intentionally or accidentally) — `find` returns the first match by **map iteration order**, which depends on DI assembly order and is not stable across builds.

2. **Wrong key for the model.** A `Map<KClass<*>, …>` strongly suggests an O(1) lookup by exact class. The `isInstance` scan converts that into an O(n) iteration that defeats the data structure. If the map actually contained the right key, `configToConnectionMap[config::class]` would be both faster and unambiguous.

A related design smell: the unchecked cast `connectionApi.value.deviceConnectionApi as? DeviceConnectionApi<API, CONFIG>` (line 47) cannot fail at runtime because of erasure — `as?` returns null only if the holder isn't a `DeviceConnectionApi` at all, which is statically impossible. The branch
```kotlin
?: throw NotImplementedError("Can't map to connection api")
```
is dead code, giving a false sense of safety.

## Reproduction / scenario
1. Suppose a future PR registers an abstract base `FTcpDeviceConnectionConfig::class` (e.g. for shared TCP behaviour) alongside `FCloudDeviceConnectionConfig::class` and `FLanDeviceConnectionConfig::class`. Both Cloud and LAN configs are TCP-derived.
2. Calling `connect(cloudConfig)` performs `find` over a `LinkedHashMap` whose order depends on which DI module was processed first. Sometimes the lookup hits `FTcpDeviceConnectionConfig` (returning whatever generic TCP API), sometimes `FCloudDeviceConnectionConfig`. Behaviour diverges between debug/release / iOS/Android builds.

## Why it happens
The author wanted to allow registration to use a class qualifier; then realised `KClass<*>` cannot be parameterised on the value type generically, so reached for `isInstance` instead of exact-class lookup.

## Impact
- Today: latent — works because no overlap exists.
- After any future inheritance refactor: silent connection to the wrong transport, or `NotImplementedError` even when the right entry is registered.
- Loss of the O(1) intended by the data structure choice.

## Suggested fix
- Use `configToConnectionMap[config::class]` for an exact-class O(1) lookup. Document the contract that registration keys must equal the runtime class, not a base.
- If sub-typing must be supported, make the structure explicit: `List<Pair<KClass<*>, …>>` and a documented "first match wins" with stable iteration order.
- Drop the dead `as?` (it cannot fail) or replace with a real `runCatching`-free typed check.
