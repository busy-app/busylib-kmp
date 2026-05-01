# Public API `tryUpdateConnectionConfig` returns `Result<Unit>` instead of `CResult<Unit>` (AGENTS.md violation)

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/FConnectedDeviceApi.kt:11-13`
- (downstream) `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:80-88`

## Summary
`FConnectedDeviceApi` lives in an `:api` module (`transport/common/api`) and is part of the public
SDK surface. AGENTS.md states:

> Return `CResult<T>` from `suspend` functions, never Kotlin's inline `Result<T>` (it does not cross the Swift boundary).

`FConnectedDeviceApi.tryUpdateConnectionConfig` returns `Result<Unit>` from a `suspend` function.
Kotlin's inline `Result<T>` is not a real class; it's an inline value class with restrictions on
where it can appear in signatures. While Kotlin allows `Result<T>` as a return type in some
positions, **it does not bridge cleanly to Swift via SKIE**. Swift consumers cannot consume this
function as written.

Furthermore, `FDeviceHolder.tryToUpdateConnectionConfig` (in `:impl`) returns `Result<Unit>`. The
orchestrator's caller (line 75-81) checks `.isSuccess` – fine for Kotlin internal use, but if the
holder method ever escapes to api-level types, the same Swift-bridge issue applies.

## Reproduction / scenario
- Try to consume `FConnectedDeviceApi.tryUpdateConnectionConfig` from Swift via the
  `BusyLibKMP.xcframework`. The function may not be visible, or it may surface as an opaque
  `KotlinResult` that's not idiomatic.

## Why it happens
- The author used Kotlin's idiomatic `Result<T>` rather than the project's `CResult<T>`. AGENTS.md
  was either not followed, or this code predates the rule.

## Impact
- Direct AGENTS.md violation.
- Swift consumers cannot use the function correctly.
- Future detekt rule (`ReturnTypeAsResultRule` or similar) will flag this; CI may go red.

## Suggested fix
- Change `FConnectedDeviceApi.tryUpdateConnectionConfig` to return `CResult<Unit>`.
- Update all implementers and callers (including `FDeviceHolder.tryToUpdateConnectionConfig`).
