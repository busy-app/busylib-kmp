# `WrappedConnectionInternal` can flip back from `Disconnected` to `Connecting`/`Connected` after scope completion, blocking auto-reconnect

## Severity
critical

## Type
infrastructure

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/WrappedConnectionInternal.kt:39-58, 90-94`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/utils/ChildSupervisorScope.kt:14-32`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/AutoReconnectConnection.kt:63-77`

## Summary
`WrappedConnectionInternal.stateFlow` is mutated from two unsynchronised sources: (1) the scope's `onCompletion` callback (which sets `Disconnected`), and (2) the public `onStatusUpdate(...)` listener method that the underlying transport invokes. Because `MutableStateFlow.value` writes are last-writer-wins and there is no causal ordering between them, an `onStatusUpdate(Connecting/Connected)` that was already in flight on another thread can run **after** the supervisor scope has completed, overwriting the terminal `Disconnected` with a non-terminal status.

When this happens, `AutoReconnectConnection`'s reconnect loop, which is waiting on `stateFlow.filter { it == Disconnected }.first()`, never observes `Disconnected`, never falls through to `connection.disconnect(); delay(...); retryCount++`, and the transport stays "connecting" forever — the system is stuck.

## Repro
Concurrent scenario (timing-sensitive but reproducible under load on a real device):

1. Underlying BLE/LAN/Cloud transport publishes `Connecting(...)` from its own thread (calls `WrappedConnectionInternal.onStatusUpdate`).
2. Connection is torn down (e.g., parent scope cancelled, or the underlying transport throws). The scope's `invokeOnCompletion` runs and writes `Disconnected` via `stateFlow.update {...}`.
3. The previously suspended `onStatusUpdate(Connecting)` resumes after the completion callback and writes `Connecting` to `stateFlow.value`.

Net effect: `stateFlow.value == Connecting`, even though the wrapped connection is dead. The encompassing `AutoReconnectConnection`'s `connection.stateFlow.filter { … Disconnected }.first()` never resolves. Reconnect never happens.

## Root Cause
1. `MutableStateFlow.update {}` and `MutableStateFlow.emit(...)` provide atomicity per call but no inter-call ordering guarantees against unrelated callers.
2. `ChildSupervisorScope` exposes the `onCompletion(t)` callback that sets `Disconnected` after the scope has been cancelled, but `onStatusUpdate` is not gated on the scope being alive — it is just a `suspend fun` that writes to the state flow regardless.
3. `ChildSupervisorScope` additionally invokes `onCompletion` twice on uncaught exceptions (once via `CoroutineExceptionHandler` and once via `job.invokeOnCompletion` after `job.cancel()`), so the terminal write happens twice but is still racy with `onStatusUpdate`.

## Impact
- **Stuck connection / no failover** — `AutoReconnectConnection` never recreates the wrapped connection, so a transport that died in this race window never recovers without a parent-scope cancellation. For combined BLE+LAN+Cloud, any single transport hitting this race blocks its own self-healing.
- The combined snapshot may keep reporting `Connecting` (priority 1) or `Connected` (priority 3) even though the underlying transport is dead, so the consumer never sees a degraded/Disconnected combined state.

## Suggested Fix
Make `onStatusUpdate` no-op once the wrapped connection has terminated, and make the terminal write idempotent:

```kotlin
private val isTerminated = atomic(false) // or simply check scope.isActive

override suspend fun onStatusUpdate(status: FInternalTransportConnectionStatus) {
    if (isTerminated.value || !scope.isActive) return
    stateFlow.update { current ->
        if (current is FInternalTransportConnectionStatus.Disconnected) current else status
    }
    yield()
}
```

And in `ChildSupervisorScope`, set `isTerminated = true` before invoking `onCompletion`, and ensure `onCompletion` is invoked exactly once (drop the duplicate call from the `CoroutineExceptionHandler`; rely solely on `invokeOnCompletion`).
