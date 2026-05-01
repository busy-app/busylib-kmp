# `StateFlow` conflation can hide an intermediate `Disconnected`, suppressing auto-reconnect

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/WrappedConnectionInternal.kt:90-94`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/AutoReconnectConnection.kt:63-72`

## Summary
`WrappedConnectionInternal.stateFlow` is a conflated `MutableStateFlow`. The `AutoReconnectConnection` reconnect loop subscribes via `connection.stateFlow.onEach { … }.filter { it == Disconnected }.first()`. Because `MutableStateFlow` only retains the latest value, a fast `Connecting → Disconnected → Connecting` sequence that happens between two scheduler ticks can be observed as just `Connecting`. The reconnect loop then never sees the `Disconnected` value and remains suspended forever.

## Repro
A flaky transport that bounces in <1 ms:
- T0: scope sets `Disconnected` (via `onCompletion`).
- T0+ε: another `onStatusUpdate(Connecting)` arrives from a stray callback (see related bug `critical_wrapped_connection_state_after_scope_completion`).

The collector is woken once and sees `Connecting`. The intermediate `Disconnected` is gone.

Even without that race, two concurrent `onStatusUpdate` calls (e.g. transport-level threading) emitting `Disconnected` then `Connecting` within the same dispatcher slot get conflated.

## Root Cause
- `MutableStateFlow.value =` and `.emit(...)` inherently conflate emissions.
- Using a `StateFlow` for a transition stream (where every transition matters) is the wrong primitive.

## Impact
- Reconnect loop stalls until something else nudges the state to `Disconnected`. In the worst case, no further nudge ever arrives.
- Combined transport sees `Connecting` indefinitely; status aggregation reports `Connecting` (priority 1) and the listener never gets a chance to recover.

## Suggested Fix
Either:

1. Use a `Channel<FInternalTransportConnectionStatus>(Channel.UNLIMITED)` or a `MutableSharedFlow(replay = 0, extraBufferCapacity = Int.MAX_VALUE, BufferOverflow.SUSPEND)` for transition events, while keeping a separate `StateFlow` for "current status".
2. In the reconnect loop, replace the "wait for the *next* `Disconnected`" check with "wait for the wrapped connection's `Job` to complete", which is the actual signal we care about:
   ```kotlin
   connection.stateFlow.onEach { … }.takeWhile { it != Disconnected }.collect()
   // or
   connection.awaitTermination()
   ```
   Where `awaitTermination()` waits on the child supervisor `Job`, which is monotonic.
