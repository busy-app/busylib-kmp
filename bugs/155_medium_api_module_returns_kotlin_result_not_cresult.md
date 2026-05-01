# Public API in `combined/api` returns Kotlin `Result<T>` instead of `CResult<T>`

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/combined/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/CombinedConnectionApi.kt:7-14`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/CombinedConnectionApiImpl.kt:24-41`

## Summary
`CombinedConnectionApi.connect` returns `kotlin.Result<FCombinedConnectionApi>`. AGENTS.md ("Hard rules for API modules") explicitly states:

> Return `CResult<T>` from `suspend` functions, never Kotlin's inline `Result<T>` (it does not cross the Swift boundary).

`Result<T>` is an inline value class. Crossing into Swift via SKIE drops the wrapping and the failure information becomes inaccessible. Any iOS consumer of the BUSY Lib KMP cannot reliably read the error from a failed `connect` call.

`FCombinedConnectionApi : FConnectedDeviceApi` inherits `tryUpdateConnectionConfig(...)` returning `Result<Unit>` from `FConnectedDeviceApi`. While this is a wider issue (the rule violation lives in `transport/common/api`), the combined module's `tryUpdateConnectionConfig` impl in `FCombinedConnectionApiImpl.kt:102-130` is also affected.

## Repro
Open `entrypoint`'s generated Swift bindings; observe that the failure path of `connect` is unrepresentable in Swift (the `Result` inline value class is not exported).

## Root Cause
The interface predates the rule, or was authored without consulting the SKIE constraints.

## Impact
- iOS / macOS consumers cannot get a typed error on `connect` failures.
- Hard rule violation per AGENTS.md, will be flagged by reviewers and in PR CI if/when a detekt rule is added.

## Suggested Fix
Replace `kotlin.Result<T>` with `CResult<T>` (the project's exported Result wrapper). This involves:

1. Updating the interface signature in `CombinedConnectionApi`.
2. Updating the `FCombinedConnectionApiImpl` and `CombinedConnectionApiImpl` implementations to return `CResult<...>`.
3. Auditing callers (e.g. higher-level features) and wrapping/unwrapping accordingly.
4. Keep internal `runSuspendCatching` use; only the public surface needs the change.

For `FConnectedDeviceApi.tryUpdateConnectionConfig`, file a follow-up to migrate the entire `transport/common/api` to `CResult` and propagate.
