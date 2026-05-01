# high — Public APIs expose bare `Flow` / `Result` instead of `WrappedFlow` / `CResult`

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/serial/FHTTPDeviceApi.kt:8-23`
  - `fun getCapabilities(): Flow<List<FHTTPTransportCapability>>` (default + interface)
  - `fun FHTTPDeviceApi.hasCapability(...): Flow<Boolean>` — extension function on the API
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/serial/FStatusStreamingApi.kt:11-13`
  - `fun getEvents(): Flow<StatusStreamingEvent>`
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/meta/FTransportMetaInfoApi.kt:9-29`
  - `fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>>` — bare `Flow` AND bare `kotlin.Result`, double violation
  - Two extension helpers (`getOrEmpty`, `getOrNullable`) likewise return bare `Flow<TransportMetaInfoData?>`

## Summary
AGENTS.md hard-rule: "Expose `WrappedFlow` / `WrappedStateFlow` / `WrappedSharedFlow`, never bare `Flow` / `StateFlow` (SKIE `FlowInterop` is intentionally disabled)." These three `:api` files break that rule. Although the `ApiWrappedTypeRule` detekt rule only triggers on modules whose path contains `/feature/api/` and therefore does **not** flag these files, the rule is still binding because `transport/common/api` types are reachable from exported XCFramework APIs (`FConnectedDeviceApi` is referenced by `FDeviceConnectStatus` in `bridge/orchestrator/api`, which is exported in `entrypoint/build.gradle.kts`). With FlowInterop turned off in SKIE, Swift consumers cannot subscribe to these flows at all — the generated symbols are opaque.

Additionally, `FTransportMetaInfoApi.get` returns `Flow<Result<Flow<…>>>`. Even on Kotlin/JVM that is awkward (Result is an inline value class — boxing/unboxing pitfalls), but on the Swift side it is unusable.

## Reproduction / scenario
- Build the XCFramework (`./gradlew :entrypoint:assembleBusyLibKMPDebugXCFramework`).
- In Swift, obtain an `FConnectedDeviceApi` (which is exported via the orchestrator API) and try to call `getCapabilities()` / `getEvents()` / `get(key:)` — the symbols are missing or surfaced as `Any` / `KotlinUnit`-typed stubs that throw.

## Why it happens
The detekt rule's path filter (`isFeatureApiModule` requires `/feature/`) was scoped narrowly when the rule was introduced; transport-layer api modules were not retrofitted, and these three files have lived under bare `Flow` since their inception.

## Impact
- iOS consumers cannot read HTTP transport capabilities, status-streaming events, or transport meta-info (battery, FW version, etc.) over the SDK boundary.
- Encourages new code to copy the same anti-pattern, since the rule does not flag it locally.

## Suggested fix
1. Change the three function signatures to return `WrappedFlow<…>` (and replace the inner `Result` with `CResult`).
2. Update implementations and extension helpers (`getOrEmpty`, `getOrNullable`, `hasCapability`) to wrap with the project's `WrappedFlow.invoke(...)` / equivalent.
3. Broaden `ApiWrappedTypeRule` so its path filter also covers `transport/*/api`, `connectionbuilder/api`, `transportconfigbuilder/api`, etc., so future regressions are caught at PR time.
