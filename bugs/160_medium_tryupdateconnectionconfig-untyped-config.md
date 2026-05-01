# medium — `tryUpdateConnectionConfig` accepts untyped `FDeviceConnectionConfig<*>` and forces brittle runtime type checks

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/FConnectedDeviceApi.kt:7-14`

## Summary
The base `FConnectedDeviceApi` interface declares
```kotlin
suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit>
```
The argument is **`FDeviceConnectionConfig<*>`** (any config type) even though every concrete implementation only accepts a single, very specific config subtype. Implementations all start with the same hand-rolled
```kotlin
if (config !is FCombinedConnectionConfig) {
    return Result.failure(IllegalArgumentException("Config $config has different type"))
}
```
or its LAN/Cloud equivalent. There is no compile-time guard preventing a caller from feeding an `FCloudDeviceConnectionConfig` to a LAN connection, or vice versa.

## Reproduction / scenario
1. Caller has an `FConnectedDeviceApi` and a fresh `FDeviceConnectionConfig<*>` from `FDeviceConnectionConfigMapper.getConnectionConfig(device)`.
2. The mapper actually returns the *combined* config; calling `tryUpdateConnectionConfig` works.
3. A future refactor adds a separate "swap to LAN-only" path; nobody updates the typing on the API; tests pass; the runtime check raises `IllegalArgumentException` at runtime, wrapped in a `Result.failure` that — per the related CResult bug — never reaches Swift.

## Why it happens
- `FConnectedDeviceApi` is parameterless on its generic; the implementing classes carry the parameter through `FDeviceConnectionConfig<API>` only at construction time. Updating the config naturally needs the same generic, but the interface erased it.
- Because of erasure, the only way to enforce type safety from inside `tryUpdateConnectionConfig` is the runtime `is` check.

## Impact
- Type errors that would have been compile-time failures become runtime `Result.failure`, often silently dropped (especially since `Result<Unit>` is the return type — see related CResult bug, the failure is invisible to Swift).
- Encourages copy-paste of the `if (config !is X) return Result.failure(...)` boilerplate in every transport.

## Suggested fix
Parameterise `FConnectedDeviceApi` on its config:
```kotlin
interface FConnectedDeviceApi<C : FDeviceConnectionConfig<*>> {
    suspend fun tryUpdateConnectionConfig(config: C): CResult<Unit>
    ...
}
```
This pushes the type check to the compiler. The cost is that `Connected.deviceApi: FConnectedDeviceApi<*>` becomes star-typed for consumers, which is acceptable — they cannot meaningfully call `tryUpdateConnectionConfig` from a star anyway, but at least the strongly-typed call site is now safe.
