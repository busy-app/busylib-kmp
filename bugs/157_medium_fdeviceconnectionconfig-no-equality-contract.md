# medium — `FDeviceConnectionConfig` has no equality contract; consumers rely on it being a data class

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/FDeviceConnectionConfig.kt:5-7`
- Use site (out of audit scope but illustrates the impact): `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/FCombinedConnectionApiImpl.kt:109-118` (`if (currentConfig == config)`)

## Summary
`FDeviceConnectionConfig<T>` is an `abstract class` with one abstract method (`getTransportTypes`). It does not declare an equality contract, and the abstract base does not override `equals`/`hashCode`. Consumers — most notably `FCombinedConnectionApiImpl.tryUpdateConnectionConfig` — guard a costly reconnect by:
```kotlin
if (currentConfig == config) {
    return Result.success(Unit)            // "configs are identical"
}
```
This works today because the four concrete configs (`FBleDeviceConnectionConfig`, `FLanDeviceConnectionConfig`, `FCloudDeviceConnectionConfig`, `FMockDeviceConnectionConfig`, plus `FCombinedConnectionConfig`) all happen to be Kotlin `data class`es. There is **nothing in the type system** that forces a future config to be a `data class` — a plain `class` would silently inherit identity-based `equals`, and the "configs are identical" short-circuit would never fire on legitimately equal configs.

The same hazard exists for `FCombinedConnectionConfig.connectionConfigs: NonEmptyList<FDeviceConnectionConfig<*>>`. `NonEmptyList` is a `data class`, so structural equality is preserved iff every contained `FDeviceConnectionConfig` honours structural equality. One non-data subclass anywhere in the list is enough to silently break the change-detection.

## Reproduction / scenario
1. Add a new transport whose config is `class FFooConfig(val host: String) : FDeviceConnectionConfig<...>`.
2. Build a `FCombinedConnectionConfig` containing `[bleConfig, fooConfig]` and call `tryUpdateConnectionConfig` with a freshly constructed but logically equal `[bleConfig, fooConfig]`.
3. Because `FFooConfig` inherits identity equality from `Any`, `currentConfig == config` returns `false`. The code path tears down and rebuilds connections every call — wasted reconnects, dropped feature streams, listener flapping.

## Why it happens
- The base class doesn't declare `equals`/`hashCode` as abstract.
- There is no detekt rule or convention enforcing `data class` for configs.

## Impact
- Today: no observable failure, since every config is a data class.
- Tomorrow: silent regressions to "tryUpdate always reconnects", which (per the existing critical bug `critical_disconnect_does_not_propagate_disconnected_status.md`) cascades into stale status reporting.

## Suggested fix
- Make `FDeviceConnectionConfig` a `sealed` hierarchy with an abstract requirement that `equals`/`hashCode` be honoured (Kotlin can't enforce data-class-ness, but a sealed root + KDoc requiring data class subclasses is at least visible).
- Add a detekt rule that checks every concrete `FDeviceConnectionConfig` is a `data class`.
- Or, switch the change-detection to a content-based comparator (e.g. compare the list of `FInternalTransportConnectionType` first, then per-transport identifiers — host, deviceId, macAddress) instead of relying on `==`.
