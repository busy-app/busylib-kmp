# high — `FLanStreamingApiImpl.getWebSocket()` leaks session if `webSocketSession()` succeeds but downstream collector throws before subscribing

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/streaming/FLanStreamingApiImpl.kt:45-57`

## Summary
```kotlin
private suspend fun getWebSocket(): Flow<Frame> {
    val session = httpClient.webSocketSession("/api/status/ws") { … }
    info { "Init websocket $session" }
    session.send(Frame.Text("{\"enable\":true}"))
    return session.incoming.receiveAsFlow().onCompletion {
        withContext(NonCancellable) {
            session.close()
        }
    }
}
```

Two leak windows exist:

1. **Cancellation between `webSocketSession(...)` returning and `session.send(...)` completing** — if the calling coroutine is cancelled at this point, `session` is created and live, but no `onCompletion` is attached because the `Flow` hasn't been built/returned yet. The session is leaked. `session.send(...)` is suspending and respects cancellation, so this window is real.

2. **Failure of `session.send(...)`** — if the initial `{"enable":true}` send fails (e.g. immediate connection close after upgrade), the function throws, `session.close()` is never called.

Additionally, `wrapWebsocket { getWebSocket() }` likely wraps the produced flow but cannot retroactively close a session that was created and never returned.

## Repro
1. Race the cancellation of the outer scope with the websocket upgrade. With slow JSON probe, the test consistently leaks the session.
2. Or use `MockEngine` to make `webSocketSession` succeed but the first `send` throw `IOException` — leaks confirmed.

## Root Cause
Resource ownership is taken in 3 phases (acquire session → send probe → wire onCompletion). Only phase-3 has a teardown.

## Impact
- Leaked WebSocket sessions accumulate over time on flaky networks.
- HTTP/WS engine resources (sockets, threads, buffers) leak.
- On Apple, this also leaks an `nw_connection_t` indirectly (depending on Ktor engine).

## Suggested Fix
Use a single try/catch around the "session created" lifetime:

```kotlin
private suspend fun getWebSocket(): Flow<Frame> {
    val session = httpClient.webSocketSession("/api/status/ws") { … }
    return try {
        session.send(Frame.Text("{\"enable\":true}"))
        session.incoming.receiveAsFlow().onCompletion {
            withContext(NonCancellable) { session.close() }
        }
    } catch (t: Throwable) {
        withContext(NonCancellable) { session.close() }
        throw t
    }
}
```

Alternatively use `flow { … emitAll(...) }` and drive the session entirely inside a single flow builder so its lifecycle is bounded by the collector.
