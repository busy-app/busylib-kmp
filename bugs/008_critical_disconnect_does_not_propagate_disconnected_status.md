# `disconnect()` leaves combined transport reporting `Connected` forever

## Severity
critical

## Type
broken-feature

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/FCombinedConnectionApiImpl.kt:153-157`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/AutoReconnectConnection.kt:100-102`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/SharedConnectionPool.kt:44-50`

## Intended behavior (per project owner)
After an explicit `disconnect()`, the public state must transition to `Disconnected` (terminal). The current behavior — keeping the last cached state, usually `Connected` — is a bug.

## Summary
After calling `FCombinedConnectionApiImpl.disconnect()`, the listener registered through `connect()` is never told the transport is `Disconnected`. The combined status remains stuck at whatever the last value was (typically `Connected`), permanently lying to upstream consumers.

## Repro
1. Successfully connect via `CombinedConnectionApi.connect(scope, config, listener, ...)` so listener observes `Connected`.
2. Call `combinedApi.disconnect()`.
3. Observe `listener.onStatusUpdate` invocations — no `Disconnected` is ever delivered.

## Root Cause
`FCombinedConnectionApiImpl.disconnect()` only forwards `disconnect()` to each `AutoReconnectConnection`:

```kotlin
override suspend fun disconnect() {
    connections.value.forEach {
        runSuspendCatching { it.disconnect() }
    }
}
```

`AutoReconnectConnection.disconnect()` calls `connectionJob.cancelAndJoin()`. That cancels the reconnect loop, but `AutoReconnectConnection.stateFlow` is never set to `Disconnected` — `MutableStateFlow` retains its **last** value across cancellation. If the last status was `Connected(...)`, that's what the flow keeps emitting.

`SharedConnectionPool.sharedState` continues collecting from each `connection.stateFlow` (it is shared eagerly in the caller-supplied scope). The status-update job in `FCombinedConnectionApiImpl` (`startCollectTransportStatusUpdateJob`) sees no change, so `listener.onStatusUpdate` is never invoked again with `Disconnected`.

There is also no explicit `listener.onStatusUpdate(Disconnected)` inside `disconnect()` to compensate.

## Impact
- High-level consumers that rely on the listener (e.g. cloud-link / UI status indicators) believe the device is still connected after the user explicitly disconnected.
- HTTP requests routed through `FCombinedHttpEngine` may continue to be dispatched to the old delegates, since `currentDelegates` is built from the same stale `Connected` snapshot.
- Re-connect attempts mistakenly think they don't need to do anything.

## Suggested Fix
Two complementary changes:

1. In `AutoReconnectConnection.disconnect()`, after `cancelAndJoin()`, force the state to `Disconnected`:
   ```kotlin
   suspend fun disconnect() {
       connectionJob.cancelAndJoin()
       stateFlow.value = FInternalTransportConnectionStatus.Disconnected
   }
   ```
   (Equivalently: register a `connectionJob.invokeOnCompletion { stateFlow.value = Disconnected }` in `init`.)

2. In `FCombinedConnectionApiImpl.disconnect()`, after disconnecting children, explicitly call
   `listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)` so the listener gets the terminal event even if the status job has already been cancelled.
