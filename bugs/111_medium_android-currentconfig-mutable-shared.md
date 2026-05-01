# `FAndroidBleApiImpl.currentConfig` is mutated without synchronisation while observed by a coroutine collector

## Type
infrastructure

**Severity:** medium

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/androidMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/api/FAndroidBleApiImpl.kt` (lines 39, 43, 50-79, 88-100)
- iOS sibling: `FIOSBleApiImpl.kt` (lines 33, 73, 75-87)

## Summary

```kotlin
class FAndroidBleApiImpl(
    …
    private var currentConfig: FBleDeviceConnectionConfig,
    …
) : FBleApi,
    FTransportMetaInfoApi by FTransportMetaInfoApiImpl(services, currentConfig.metaInfoGattMap),
    …
{
    override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
        …
        if (currentConfig.copy(deviceName = config.deviceName) == config) {
            currentConfig = config        // ← unsynchronised write
            return Result.success(Unit)
        }
        …
    }
}
```

`currentConfig` is referenced from:

- The constructor body of the primary `FTransportMetaInfoApiImpl`
  delegate — captured **once** by value at construction time. Updating
  `currentConfig` later does **not** update the meta-info delegate's
  copy.
- `startPeripheralStatusCollectionJob`, which uses
  `currentConfig.getTransportTypes()` inside a `combine.transform` —
  read on a coroutine scheduler thread.
- `tryUpdateConnectionConfig` writes — typically called on the public
  feature scope.

Two concrete bugs:

1. **Stale `metaInfoGattMap`.** The property delegate
   `FTransportMetaInfoApi by FTransportMetaInfoApiImpl(services,
   currentConfig.metaInfoGattMap)` captures the *initial* map. If a
   consumer calls `tryUpdateConnectionConfig` (today only allowed for
   `deviceName` changes — which is fine), but a future caller widens the
   "non-name fields" check to include `metaInfoGattMap`, the delegate
   silently keeps using the stale map.

2. **Data race on `currentConfig`.** Plain `var` is read from the
   `combine` collector and written from
   `tryUpdateConnectionConfig`. JVM happens-before guarantees nothing
   without `@Volatile` or atomic. The read in `combine.transform` may
   observe a torn / stale reference. (For an immutable data class
   reference this is mostly benign, but reads can intermittently see
   `null` on Kotlin/Native.)

## Reproduction

For (1), once a future change permits non-name updates, the stale map is
silently used. For (2), a code-static-analyser run (`-Xstrict-mode`) will
flag the unsynchronised access.

## Root cause

`currentConfig` is treated as immutable for capture purposes, but the
class also exposes a setter; the captures do not refresh.

## Impact

- Latent stale-state bug.
- Race-condition warning for tooling.

## Suggested fix

- Replace `private var currentConfig` with `private val currentConfig =
  MutableStateFlow(initial)`; expose `currentConfig.value` for reads and
  use `update { … }` for writes.
- Pass a flow/lambda for `metaInfoGattMap` to
  `FTransportMetaInfoApiImpl` so it always reads the latest value:

  ```kotlin
  FTransportMetaInfoApiImpl(services, ::currentMetaInfoGattMap)
  // where
  private fun currentMetaInfoGattMap() = currentConfig.value.metaInfoGattMap
  ```
