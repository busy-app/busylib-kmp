# `runCatching` in suspend function violates project rule and swallows cancellation

## Severity
high

## Type
infrastructure

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/orchestrator/ActiveWebSocketHolder.kt` (lines 75-90)

## Summary
`safeSend` uses Kotlin's stdlib `runCatching` inside a suspend block, with the
project's custom rule explicitly suppressed:

```kotlin
@Suppress("RunCatchingInSuspendRule")
return runCatching { // By design ignore cancellation exception from webSocketApi.send
    withTimeout(SEND_TIMEOUT) {
        webSocketApi.send(request)
    }
}.onFailure { logger.error(it) { "Failed to send request $request" } }
```

`runCatching` catches **`CancellationException`** as well as ordinary `Throwable`. The
inline comment says "by design", but the consequences are worse than the comment
suggests — see Impact.

The `AGENTS.md` hard rule is: "No `runCatching` inside `suspend` functions — use
`runSuspendCatching` instead." The detekt rule `RunCatchingInSuspendRule` is suppressed
locally rather than fixed.

## Repro
1. Cancel the parent coroutine that is collecting `getEventsFlow(...)` while a
   `safeSend(SubscribeState(...))` is in flight (between `.send(...)` invocation and
   acknowledgement).
2. The `withTimeout`-wrapped block is cancelled, throwing `TimeoutCancellationException`
   (or a regular `CancellationException` from the parent).
3. `runCatching` swallows it; `safeSend` returns `Result.failure(...)` and the
   `withLock` block keeps running. The `currentWebSocket` may be left stale, and the
   `mutex` continues holding work past the cancellation point. The `withLock` does
   eventually rethrow on its `ensureActive` checks at suspend points, but only after
   touching mutable state with a now-cancelled context.

## Root Cause
- `runCatching` does not honor cooperative cancellation — it catches every Throwable
  including `CancellationException` (and the `TimeoutCancellationException` subclass
  raised by `withTimeout`).
- The intent ("ignore cancellation from ws.send") could be achieved with
  `runSuspendCatching`, which rethrows cancellation, plus a separate `try/catch
  (e: TimeoutCancellationException)` if needed.

## Impact
- Parent cancellation is delayed/lost while the holder mutex is held.
- Detekt rule is being suppressed in API impl code, defeating its purpose.
- If the consumer scope is cancelled, the holder mutates `activeSubscriptionsSet` and
  `currentWebSocket` under a cancelled context, leaking inconsistent state into the
  next reconcile.

## Suggested Fix
Replace with `runSuspendCatching`:

```kotlin
return runSuspendCatching {
    withTimeout(SEND_TIMEOUT) { webSocketApi.send(request) }
}.onFailure { logger.error(it) { "Failed to send request $request" } }
```

Remove the `@Suppress("RunCatchingInSuspendRule")`. If you specifically want to swallow
`TimeoutCancellationException` but propagate other cancellation, catch it explicitly:

```kotlin
return runSuspendCatching {
    try {
        withTimeout(SEND_TIMEOUT) { webSocketApi.send(request) }
    } catch (e: TimeoutCancellationException) {
        // tolerate as failure
    }
}
```
