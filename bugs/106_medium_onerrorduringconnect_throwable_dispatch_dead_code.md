# `FTransportListenerImpl.onErrorDuringConnect` has dead `when (throwable)` dispatch

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FTransportListenerImpl.kt:25-38`

## Summary
The `when (throwable)` block contains only an `else` branch. The `@Suppress("UNUSED_EXPRESSION")`
comment confirms the author knows the `when` is functionally dead, but the structure suggests
intent to add error-class-specific behavior (timeout vs auth-failure vs hardware-error) that
never landed. As a result, every error – regardless of root cause – is reported as
`DisconnectStatus.ERROR_UNKNOWN`. Consumers cannot distinguish "timeout, please retry" from
"device denied authentication, please reset" from "hardware crashed, contact support".

This combines with `DisconnectStatus` having only three values (`NOT_INITIALIZED`,
`REPORTED_BY_TRANSPORT`, `ERROR_UNKNOWN`) – so the user never gets useful diagnostic info.

## Reproduction / scenario
- Connect attempt fails for any reason. Subscribers always see `Disconnected(reason =
  ERROR_UNKNOWN)`. Higher-level UI cannot show actionable error messages.

## Why it happens
- Placeholder code that was never filled in.

## Impact
- Poor user experience: "Something went wrong" generic error for every failure mode.
- Difficult debugging in production: telemetry only knows ERROR_UNKNOWN.

## Suggested fix
- Either remove the `when` entirely (just call `state.update { Disconnected(...) }` with
  ERROR_UNKNOWN) and accept the limitation, OR populate it with discriminated branches:
  - `is CancellationException` (skip, it's a deliberate cancel)
  - `is TimeoutCancellationException` → ERROR_TIMEOUT
  - `is IOException` → ERROR_IO
  - `is AuthException` → ERROR_AUTH
  - else → ERROR_UNKNOWN
- And expand `DisconnectStatus` to support the new reasons.
