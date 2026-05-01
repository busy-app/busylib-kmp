# `connectIfNot` returns `Unit` and silently swallows mapper failures

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/internal/src/commonMain/kotlin/net/flipper/bridge/connection/orchestrator/internal/FInternalDeviceOrchestrator.kt:9-10`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:68-116`

## Summary
`connectIfNot(config: BUSYBar)` is `suspend ... : Unit`. There is no return value. Errors from
`deviceConnectionConfigMapper.getConnectionConfig(config)` (line 71) are not caught – if the
mapper throws, the exception propagates straight out of `connectIfNot` (and out of `withLock`),
killing the caller's coroutine.

Worse, *partial* failures (e.g., listener emit succeeds but holder build throws) leave
`transportListenerFlow` updated with the new listener but `currentDevice` left as the old one.

## Reproduction / scenario
1. Caller has a healthy connection to deviceA.
2. Caller invokes `connectIfNot(deviceB)` – a config that the mapper cannot build (e.g., missing
   credential, malformed BUSYBar).
3. `getConnectionConfig` throws.
4. Exception propagates out of `connectIfNot` *before* `disconnectInternalUnsafe` is called.
5. `currentDevice` still references holder for deviceA. Public state still shows Connected to
   deviceA. Caller sees an exception bubble up.

OR:
1. The mapper succeeds. `disconnectInternalUnsafe()` runs and tears down deviceA. New listener
   is emitted to `transportListenerFlow` (line 87). Public state moves to default (Disconnected,
   NOT_INITIALIZED).
2. `deviceHolderFactory.build(...)` throws (e.g., factory dependency injection failure, or
   `getConnectionConfig` is called twice and throws on second call).
3. `currentDevice` is left at `null` (because `disconnectInternalUnsafe` set it to null and the
   new assignment never executed).
4. Public state stuck at the default Disconnected.
5. Caller's coroutine receives the exception.

The user is dropped from deviceA without ever connecting to deviceB, and the state flow doesn't
reflect any meaningful information.

## Why it happens
- No try/catch around the build path.
- No partial-state recovery – the design assumes "either everything succeeds or the caller deals
  with the exception".

## Impact
- Caller-facing exceptions for what should be reportable failures (return Result.failure or emit
  Disconnected with reason).
- Implicit disconnect from the previous device when a new connect attempt fails – data loss.

## Suggested fix
- Wrap the build in `runSuspendCatching` and recover gracefully on failure: re-emit the previous
  `currentDevice` (or null) and set state to a meaningful Disconnected reason.
- Consider returning `CResult<Unit>` from `connectIfNot` so callers can react to failures
  programmatically.
- At minimum, do not destroy the previous connection until the new one is fully built.
