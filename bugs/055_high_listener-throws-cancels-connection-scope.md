# high — A listener that throws (or is cancelled) tears down the entire connection scope

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/FTransportConnectionStatusListener.kt:3-5`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/FCombinedConnectionApiImpl.kt:73-97` (listener invocation point — outside the explicit scope of this audit but relevant for impact)
- `components/bridge/connectionbuilder/impl/src/commonMain/kotlin/net/flipper/bridge/connection/connectionbuilder/impl/FDeviceConfigToConnectionImpl.kt:24-52` (the contract surface)

## Intended behavior (per project owner)
Listener errors should be **isolated and logged**, not propagated through the connection scope. A buggy consumer must not be able to take down the entire connection.

## Summary
`FTransportConnectionStatusListener.onStatusUpdate` is a **suspending** callback. Every transport implementation invokes it inside its own connection scope (e.g. `FCombinedConnectionApiImpl.startCollectTransportStatusUpdateJob` does `.onEach { listener.onStatusUpdate(...) }.launchIn(scope)`). If the listener throws — or, worse, if the listener's caller cancels its own scope from inside `onStatusUpdate` — the exception propagates back into the transport's flow, which kills the `launchIn(scope)` job. Because that scope is the connection's primary scope (passed all the way down from `FDeviceConfigToConnection.connect`), an uncatched throw from the listener cancels every job rooted in that scope: status updates, capability flows, the HTTP engine, status streaming, the auto-reconnect loop. The connection silently dies and the only signal the host gets is the original exception that the listener itself threw.

There is no `try { listener.onStatusUpdate(...) } catch { … }` anywhere along this path; nothing isolates the listener from the transport.

## Reproduction / scenario
1. Host app passes a listener that, on `Connected`, queries shared state and throws (e.g. NPE on a recycled `ViewModel`).
2. The throw resumes inside `onEach` → cancels the `launchIn` job → cancels its parent (the connection `scope`) because there is no `SupervisorJob` between them → all flows die.
3. Subsequent `tryUpdateConnectionConfig`, `getCapabilities()`, `getEvents()` calls find their flows already terminated and never emit.
4. The host has no way to recover without calling `connect(...)` again.

## Why it happens
- The listener interface declares `suspend fun onStatusUpdate(...)` with no documented "must-not-throw" contract. Listener implementations are user code.
- The transport collects the listener inside the connection's primary scope without wrapping with `supervisorScope { ... }` or guarding the invocation with `runSuspendCatching { listener.onStatusUpdate(...) }`.
- The shared scope means a failure in the listener path is indistinguishable from a transport-level fatal error.

## Impact
- A single buggy listener crashes the entire SDK session for that device — no reconnect, no metadata, no capabilities.
- Hard to diagnose because the stack trace surfaces in the listener but the symptom is "everything stopped".
- Especially dangerous for iOS clients: Swift `Task` cancellation propagates as `CancellationException` into `onStatusUpdate`, which is *not* caught (well-behaved) — but any other Swift error becomes an `NSError` ↔ Kotlin `Throwable` and brings down the connection.

## Suggested fix
1. Document that `onStatusUpdate` must not throw, and wrap the call site with `runSuspendCatching { listener.onStatusUpdate(...) }` so listener failures are isolated and just logged.
2. Optionally, run the listener inside a child `SupervisorJob` of the connection scope so that even a cancelled listener (e.g. host-cancelled) does not destroy the connection scope.
3. Consider pushing status updates through a `WrappedStateFlow` exposed by the API, and let consumers `collect` it in their own scope; the SDK would no longer call user code on its own scope at all.
