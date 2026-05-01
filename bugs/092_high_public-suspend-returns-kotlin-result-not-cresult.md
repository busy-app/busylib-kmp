# high — Public `suspend fun connect/tryUpdateConnectionConfig` returns `kotlin.Result` instead of `CResult`

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/LanDeviceConnectionApi.kt:8-13`
- `components/bridge/transport/tcp/cloud/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/cloud/api/CloudDeviceConnectionApi.kt:8-13`
- (Root cause is in the parent: `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/DeviceConnectionApi.kt:5-11`)
- Implementations: `LanDeviceConnectionApiImpl.kt:16-29`, `CloudDeviceConnectionApiImpl.kt:23-38`, `FLanApiImpl.tryUpdateConnectionConfig` (lines 57-69), `FCloudApiImpl.tryUpdateConnectionConfig` (lines 54-66)

## Summary
AGENTS.md is unambiguous:
> "Return `CResult<T>` from `suspend` functions, never Kotlin's inline `Result<T>` (it does not cross the Swift boundary)."

Every public-facing `suspend fun ...: Result<...>` in these `:api` modules violates this — `Result<FLanApi>`, `Result<FCloudApi>`, `Result<Unit>`. `kotlin.Result` is a value class; it is erased to `Object` on JVM and is not exposed by SKIE to Swift. iOS callers either get an opaque `Any` or a generated wrapper that throws on access.

## Repro
Open the generated `BusyLibKMP.xcframework` in Xcode and try to call `LanDeviceConnectionApi.connect(...)` from Swift. The return type is unusable.

## Root Cause
`DeviceConnectionApi` (in `transport/common/api`) was authored before the `CResult` rule, and the LAN/Cloud `:api` modules just narrowed the bound. The rule was added but not retrofitted.

## Impact
- **Swift consumers cannot meaningfully consume the result** — failure cases are silent.
- Implementations are forced to use `runSuspendCatching { ... }: Result<T>` which then needs `.toCResult()` everywhere downstream — wasted plumbing and risk of dropping errors.
- `tryUpdateConnectionConfig` calls return `Result.failure(IllegalArgumentException(...))` — those exceptions never reach the iOS side.

## Suggested Fix
1. Change `DeviceConnectionApi.connect(...)` and `FConnectedDeviceApi.tryUpdateConnectionConfig(...)` signatures to `CResult<API>` / `CResult<Unit>` (this is a public-API break — flag in release notes).
2. Update both impl modules: replace `runSuspendCatching {...}` returns with `.toCResult()` (already in `core/wrapper`).
3. Add a detekt rule (or extend `ForbiddenPublicApiResultRule` if present) to forbid `Result<*>` return types in `:api` modules.
