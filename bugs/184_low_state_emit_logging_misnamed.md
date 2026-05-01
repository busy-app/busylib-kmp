# `FTransportListenerImpl.onErrorDuringConnect` uses `state.update` (no `info` log) while `onStatusUpdate` logs

## Severity
low

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FTransportListenerImpl.kt:25-38`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FTransportListenerImpl.kt:40-72`

## Summary
`onStatusUpdate` ends with `info { "New state is $newState" }`. `onErrorDuringConnect` does not
log the new state – only the error. As a result, debugging traces show "New state is X" entries
intermixed with error logs that don't have a "New state is Y" follow-up. Inconsistent logging
makes it harder to trace transitions.

## Reproduction / scenario
- Read `FTransportListenerImpl-<uniqueId>` log lines after a connect-error. The error is logged
  but not the resulting state transition.

## Why it happens
- `onErrorDuringConnect` uses `state.update` (no return value), `onStatusUpdate` uses
  `state.updateAndGet` (capturing the new value for the log).

## Impact
- Cosmetic: harder debugging.

## Suggested fix
Switch to `state.updateAndGet` and log the resulting state:

```kotlin
val newState = state.updateAndGet { ... }
info { "New state is $newState" }
```
