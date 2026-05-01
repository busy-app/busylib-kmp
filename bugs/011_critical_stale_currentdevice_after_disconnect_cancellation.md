# `currentDevice` is left in a stale state when `disconnectInternalUnsafe` throws

## Severity
critical

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:133-145`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:90-99`

## Summary
`disconnectInternalUnsafe` calls `currentDeviceLocal.disconnect()` and then assigns
`currentDevice = null` *after* that call. Because `FDeviceHolder.disconnect()` can throw
(see `critical_disconnect_throws_cancellation_when_called_during_connecting.md`), the assignment
never happens. The orchestrator is left believing `currentDevice` is still the (partially torn
down) old holder. The next call to `connectIfNot` may match `currentDeviceHolder.uniqueId ==
config.uniqueId`, attempt `tryToUpdateConnectionConfig` against a half-dead holder, fall through to
another `disconnectInternalUnsafe` which throws *again*, and so on – a permanent stuck state.

## Reproduction / scenario
1. Connect to device A; while still in `Connecting`, call `disconnectCurrent()`.
2. `disconnect()` throws CancellationException (see critical bug 1).
3. `currentDevice` is *not* set to null, mutex is released.
4. User taps "Connect" again on the same device A. `connectIfNot(configA)` enters mutex,
   `currentDevice` still references the dead holder.
5. `currentDeviceHolder.tryToUpdateConnectionConfig(connectionConfig)` runs `getCompleted()` on a
   cancelled `Deferred` – returns `Result.failure` (good) but only because the cancellation in
   `getCompleted()` is wrapped here in a fresh `runSuspendCatching` that catches it.
6. Falls through to `disconnectInternalUnsafe()` which calls `currentDeviceLocal.disconnect()` on
   the same dead holder. `cancelAndJoin` is now a no-op on an already-cancelled Deferred, and
   `getCompleted()` again throws CancellationException → propagates out → the user is permanently
   unable to reconnect without re-creating the orchestrator graph (which is `@SingleIn(BusyLibGraph)`,
   so basically restarting the app).

## Why it happens
- The cleanup ordering inside `disconnectInternalUnsafe` is "do work first, then null out state".
  Combined with `disconnect()` being able to throw, state is never cleared on the failure path.
- There is no `try { ... } finally { currentDevice = null }` or equivalent.

## Impact
- After a single mistimed disconnect-during-connect, the orchestrator is dead until the process is
  restarted. Repeating connect attempts always crash with CancellationException.
- Any service depending on `getState()` keeps showing whatever the last emitted status was, even
  though the underlying state machine is broken.

## Suggested fix
Always null out `currentDevice` in a `finally`, regardless of whether `disconnect()` threw:

```kotlin
private suspend fun disconnectInternalUnsafe(configToDisconnect: FDeviceHolder<*>? = null) {
    val currentDeviceLocal = currentDevice
    if (configToDisconnect != null && currentDeviceLocal !== configToDisconnect) {
        info { "Tried to disconnect not current device, skip" }
        return
    }
    try {
        currentDeviceLocal?.disconnect()
    } finally {
        if (currentDevice === currentDeviceLocal) {
            currentDevice = null
        }
    }
}
```

Together with fixing `FDeviceHolder.disconnect()` to not throw on cancelled-while-connecting.
