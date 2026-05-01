# `tryToUpdateConnectionConfig` calls `getCompleted()` on a possibly non-completed Deferred

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:80-88`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:74-82`

## Summary
`FDeviceHolder.tryToUpdateConnectionConfig` does `runSuspendCatching { deviceApi.getCompleted() }`.
`Deferred.getCompleted()` throws `IllegalStateException` if the Deferred has not yet completed.
That works as a guard *only by accident* – it forces the orchestrator into the "fall through to
full reconnect" branch, which immediately invokes `disconnectInternalUnsafe` on the still-connecting
holder and triggers the cascading failure described in
`critical_disconnect_throws_cancellation_when_called_during_connecting.md`.

## Reproduction / scenario
1. Caller invokes `connectIfNot(configA)`; the holder is created and `deviceApi` (an `async`) is
   running, hasn't returned yet.
2. While the connection is still in progress, the caller invokes `connectIfNot(configA)` again
   (same uniqueId, possibly same or slightly different config – e.g. an updated LAN address).
3. `currentDeviceHolder.uniqueId == config.uniqueId` → orchestrator calls
   `tryToUpdateConnectionConfig(connectionConfig)`.
4. `deviceApi.getCompleted()` throws `IllegalStateException("This deferred value has not completed yet")`.
5. `runSuspendCatching` catches the (non-cancellation) throwable → returns `Result.failure`.
6. Orchestrator logs `"Failed to update current connect, request full reconnection"` and falls
   through to `disconnectInternalUnsafe()` → `currentDeviceLocal.disconnect()` → cancels the
   in-flight async → see the cascading critical bug.

## Why it happens
- `getCompleted()` is documented to throw if the Deferred isn't completed. The author should be
  using `await()` (suspend until completion) or a non-throwing peek (`isCompleted` + `getCompleted`)
  – but neither matches the intent here ("test if a hot connection exists, if not give up").
- The intent appears to be "synchronously check whether we already have a usable API; if we don't,
  fall through". The right primitive is `if (deviceApi.isCompleted) deviceApi.getCompleted() else
  null`, not `runSuspendCatching { getCompleted() }`.

## Impact
- A second `connectIfNot(sameDevice)` issued before the first one completes will always force a
  full disconnect/reconnect cycle, even when the device API would have happily applied the new
  config a few hundred milliseconds later.
- Combined with the disconnect-throws bug, this turns a perfectly reasonable user behavior ("retry
  connect because UI re-rendered") into a hard wedge of the orchestrator.

## Suggested fix
Use the non-throwing accessor and keep the suspend semantics explicit:

```kotlin
suspend fun tryToUpdateConnectionConfig(
    config: FDeviceConnectionConfig<*>
): Result<Unit> {
    val api = if (deviceApi.isCompleted && !deviceApi.isCancelled) {
        runSuspendCatching { deviceApi.getCompleted() }.getOrNull()
    } else null
    return if (api == null) {
        Result.failure(IllegalStateException("Device not yet connected"))
    } else {
        api.tryUpdateConnectionConfig(config)
    }
}
```
