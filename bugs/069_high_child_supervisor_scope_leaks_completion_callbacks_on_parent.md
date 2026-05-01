# `ChildSupervisorScope` leaks `invokeOnCompletion` registrations on the parent scope and double-fires `onCompletion`

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/utils/ChildSupervisorScope.kt:14-32`

## Summary
`ChildSupervisorScope` registers an `invokeOnCompletion` handler on the **parent** scope's `Job` for every wrapped connection and never disposes it. With `AutoReconnectConnection` recreating the wrapped connection on every disconnect, the parent scope accumulates one new `invokeOnCompletion { job.cancel() }` registration per reconnect cycle.

Additionally, `onCompletion` runs twice on uncaught exceptions: once from the `CoroutineExceptionHandler` and once from `job.invokeOnCompletion`, after `job.cancel()`.

## Repro
A long-lived caller scope, paired with a flaky transport that reconnects 100 times, leaves 100 zombie `invokeOnCompletion` handlers on the parent scope. Each handler holds a reference to its now-dead child `Job`. Memory grows linearly with reconnects.

For the double-fire: any uncaught throwable inside the wrapped connection's coroutines triggers `CoroutineExceptionHandler` → `onCompletion(throwable)` → `job.cancel()` → `invokeOnCompletion(throwable)` → `onCompletion(throwable)` again. Logs become misleading; if `onCompletion` performs side effects (it sets `stateFlow` to `Disconnected` in `WrappedConnectionInternal`), they are duplicated.

## Root Cause
```kotlin
parentScope.coroutineContext[Job]?.invokeOnCompletion {
    job.cancel()
}
```

This returns a `DisposableHandle` that is silently dropped. There is no symmetric `job.invokeOnCompletion { handle.dispose() }` to clean it up when the child completes (the normal case).

The `CoroutineExceptionHandler` calls `onCompletion(throwable)` directly **and** `job.cancel()`, even though `job.cancel()` will trigger the same `onCompletion` via the registered `invokeOnCompletion`.

## Impact
- Memory growth proportional to reconnect count over the lifetime of the parent scope.
- Spurious double execution of completion side effects (extra `Disconnected` writes, duplicate logs, misleading stack traces).
- In long-lived processes (Android Service, daemon, CLI) this is a slow leak that's hard to reproduce in tests.

## Suggested Fix
1. Capture and dispose the parent handle on child completion:
   ```kotlin
   val parentHandle = parentScope.coroutineContext[Job]?.invokeOnCompletion { job.cancel() }
   job.invokeOnCompletion { parentHandle?.dispose() }
   ```
2. Pick exactly one path for handling uncaught exceptions. Either:
   - Drop the `CoroutineExceptionHandler` entirely and rely on `invokeOnCompletion` to receive the cause; or
   - Inside the handler, do not call `onCompletion` directly — only call `job.cancel(cause)` and let `invokeOnCompletion` deliver it once.
