# `FDeviceHolder.init.invokeOnCompletion` short-circuits errors and never updates the listener for non-cancellation throwables

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:101-119`

## Summary
The `invokeOnCompletion` handler distinguishes:
- `null` (normal completion) and `CancellationException` → call listener with `Disconnected`.
- any other throwable → log "Status update will be handled inside exception handler", do
  *nothing* else.

The "exception handler" referenced here is the `CoroutineExceptionHandler` configured at line
65-67. That handler is invoked *only* when an exception is unhandled by any coroutine in the
scope. Specifically, `scope.async { ... }` does *not* propagate its exception through the
exception handler – an `async`'s exception is stored in the Deferred and surfaced via
`await()`/`getCompleted()`. So an exception that originates inside the `deviceApi` async will:

1. Never reach the `CoroutineExceptionHandler` (because it is contained by the Deferred).
2. Cause the scope's job to *not* complete with an error – the scope job remains alive (the only
   completed thing is the Deferred).
3. ... unless the user cancels the scope, in which case `invokeOnCompletion` will run with
   `CancellationException` (going through the "Disconnected" branch).

So the "non-null, non-CancellationException" branch is *unreachable* in normal operation when the
exception comes from the deviceApi async. It would only fire if some other coroutine launched on
the scope threw an unhandled non-cancellation exception – which is uncommon.

The bigger issue: when `deviceApi` fails with a real error (e.g. `IOException` from
`deviceConnectionHelper.connect`), the chain goes:

1. `connect(...).getOrThrow()` throws.
2. Before throwing, `onFailure { onConnectError(this, t) }` fires – good, error is reported.
3. `getOrThrow()` rethrows; the `async` Deferred completes exceptionally.
4. The scope's job is *not* cancelled by this (async exceptions are contained).
5. `invokeOnCompletion` does NOT fire, because the scope is still alive.
6. The orchestrator's listener path receives `onErrorDuringConnect(...)` from step 2 and
   transitions to `Disconnected(ERROR_UNKNOWN)`. OK.
7. But the holder's scope is *never cancelled* – so the `Job` and any other children keep
   running. The holder is left in a zombie state.

The orchestrator does eventually clean up via `onInternalDisconnect`, which calls
`disconnectInternalUnsafe(deviceHolder)` → `currentDeviceLocal.disconnect()` → which finally
cancels the scope. So the leak is bounded.

But: `onConnectError` is registered to be called only *once* (on the first failure). If something
subsequently throws inside the scope (via a child job), no listener is notified. The
"#init catch error during invokeOnCompletion" log would fire, with the comment that
"Status update will be handled inside exception handler" – which, as established, never happens
for async-contained exceptions.

## Reproduction / scenario
1. Setup: `deviceConnectionHelper.connect` succeeds and returns an API. Then a child coroutine
   started by `connect` throws after some time (e.g., a heartbeat poller hits an IOException).
2. The exception is not caught locally. It bubbles up to the
   `CoroutineExceptionHandler`, which runs `exceptionHandler(this, throwable)` → calls
   `onInternalDisconnect`. Listener gets `Disconnected(ERROR_UNKNOWN)`.
3. The scope's job is now cancelled (because an unhandled exception in a child cancels its
   parent).
4. `invokeOnCompletion` fires with `t = the original throwable`.
5. The "non-null, non-cancellation" branch runs: just logs an error. Listener is not invoked
   again.

OK – in this case the exceptionHandler did the listener notification.

But: consider the case where the orchestrator's `exceptionHandler` itself throws (e.g., during
its own `globalScope.launch { ... }` setup, or during `localTransportListener.
onErrorDuringConnect`). The error is swallowed by `globalScope`'s default exception handling
(printed to console, nothing more). The listener is never notified, and the scope's
`invokeOnCompletion` only logs.

## Why it happens
- The `init` block makes assumptions about which path will fire and trusts the
  `CoroutineExceptionHandler` to "handle the rest". But the actual exception flow for `async`
  Deferreds doesn't match those assumptions.

## Impact
- Edge case: if the exceptionHandler throws or fails to deliver the error, the listener is
  silently never notified of the failure. Public state appears "stuck" in `Connected` /
  `Connecting`.

## Suggested fix
- Always call the listener with `Disconnected` (or a derived error status) from
  `invokeOnCompletion`, regardless of whether the cause is a `CancellationException` or a real
  error. Then de-dup downstream (see other bug reports about duplicate Disconnected emissions).
- Also add a regression test that the listener is *always* notified of disconnect, no matter
  which path triggered it.
