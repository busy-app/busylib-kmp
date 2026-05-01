# `TransformWhileSubscribedSharedFlow` leaks `replayCache` and a stale `replayCacheResetJob` if the scope is cancelled mid-grace-period

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/TransformWhileSubscribedSharedFlow.kt` lines 80–94, 130–132

## Summary
The class manages two background jobs (`transformFlowCollectorJob`, `replayCacheResetJob`) launched on the user-supplied `scope`. If `scope` is cancelled while the flow has 0 subscribers and is in the grace period (after the last subscriber left, before timeout fires), the `replayCacheResetJob` gets cancelled mid-`delay(timeoutDuration)`, **before** running `resultFlow.resetReplayCache()`.

```kotlin
private suspend fun startReplayCacheResetJobUnsafe() {
    debug { "#startReplayCacheResetJobUnsafe" }
    replayCacheResetJob?.cancelAndJoin()
    replayCacheResetJob = scope.launch {
        delay(timeoutDuration)              // ← cancelled if scope dies
        resultFlow.resetReplayCache()       // ← never runs in that case
        upstreamSharedFlow.replayCache      // ← dead expression, also never runs
    }
}
```

Consequences:
1. `resultFlow.replayCache` keeps the last value alive *as a strong reference inside the SharedFlow*. If the flow is restarted by sharing (`replay = 1`), the next subscriber receives the stale value as if it were fresh.
2. The trailing expression `upstreamSharedFlow.replayCache` has no consumer — it's a dead expression (probably leftover debug code).
3. The class also implements `LogTagProvider by TaggedLogger("TransformWhileSubscribedSharedFlow")` — fine — but the constructed `TaggedLogger` instance is **per-flow-instance**, not shared, so the log tag is correct.

The main bug here is that the start of `startSubscriberCountJob` on line 131 in `init { ... }` does not handle the case where the parent scope is cancelled. The `hasSubscribersFlow.onEach { ... }.launchIn(scope)` will silently terminate — the flow is now a "dead" instance that emits nothing on subscription. There is no detection, no error, no log.

## Repro
```kotlin
val scope = CoroutineScope(Job())
val tw = MutableStateFlow(0).transformWhileSubscribed(timeout = 30.seconds, scope = scope) { it }
val job = launch { tw.first() }            // subscribe + receive
job.cancel()
delay(5_000)                                // halfway through grace period
scope.cancel()                              // tear down before timeout
// resultFlow.replayCache still holds the value 0 forever
// any new collector (if scope were revived) would see stale 0
```

In this codebase, `scope` is typically the device-lifecycle scope; cancelling a device while a flow is in grace period leaves the cached value alive for the lifetime of the JVM/process — small leak per device.

## Root Cause
- Resetting the replay cache is gated on a coroutine that gets cancelled with the scope.
- No `try/finally` ensures the cache is reset on cancellation.
- Dead-expression `upstreamSharedFlow.replayCache` suggests the cleanup was never implemented.

## Impact
- A leaked replay value per `transformWhileSubscribed` invocation that ends mid-grace-period.
- New subscribers (if the flow object outlives the scope, which it can if any consumer holds a reference) see stale cached values that should have been cleared.
- Subtle correctness bug for "transform" flows representing transient state (e.g. last RPC error).

## Suggested Fix
Use `try/finally` to ensure cache reset on cancellation, and remove the dead expression:

```kotlin
private suspend fun startReplayCacheResetJobUnsafe() {
    replayCacheResetJob?.cancelAndJoin()
    replayCacheResetJob = scope.launch {
        try {
            delay(timeoutDuration)
        } finally {
            // Always reset on completion — including cancellation
            resultFlow.resetReplayCache()
        }
    }
}
```
Alternatively, register a `scope.coroutineContext.job.invokeOnCompletion { resultFlow.resetReplayCache() }` in `init`.
