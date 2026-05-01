# `wrapWebsocket` increments `retryCount` twice on every failure → exponential delay grows too fast

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/WebSocketKtx.kt` lines 19–43

## Summary
On every loop iteration, `wrapWebsocket` runs `block().catch { retryCount++ }` *inside* `runSuspendCatching { coroutineScope { ... } }`. If the inner flow fails, `.catch` swallows it and increments `retryCount`. But if the flow's exception escapes the `coroutineScope` (e.g. an exception thrown by an inner `launch`-spawned `writeJob` of the OkHttp engine), `runSuspendCatching.onFailure` catches it and **also** increments `retryCount`. Both branches are "the same retry".

```kotlin
runSuspendCatching {
    coroutineScope {
        block().catch {
            retryCount++                                   // (a)
            error(it) { "Failed request websocket" }
        }.collect { … retryCount = 0 … }
    }
}.onFailure { e ->
    retryCount++                                           // (b)
    error(e) { "Failed request websocket" }
}
```

Even when the flow fails through path (a), the `coroutineScope` may also rethrow if any sibling fails, hitting (b). The end result is `retryCount` advancing by 2 per failure in the common case and the backoff escalating to the 10 s cap after only 2 real failures instead of ~5.

## Repro
1. Start the websocket on an unreachable host.
2. Observe the timing of reconnect attempts: 1s, 4s, 10s, 10s… instead of 1s, 2s, 4s, 8s, 10s.

## Root Cause
The two error-handling layers (Flow `.catch` and outer `runSuspendCatching`) are independent and can both fire for a single underlying failure. Additionally, `retryCount = 0` is reset only inside `.collect { … }`, so a successful-then-failed cycle advances count correctly only once per cycle, but a failure that goes through both paths advances by 2.

## Impact
- Dramatically faster decay of useful retry attempts.
- Effective "fail" timeout is reached after fewer real attempts.
- Combined with `getExponentialDelay(retryCount)` `factor.pow(retryCount)`, the count overflowing past `~30` triggers `Double.POSITIVE_INFINITY` arithmetic on `delay()` (see `RetryKtx`); long-running websocket sessions with many small disconnects may overflow eventually (see also bugs/high_retry-pow-overflow.md).

## Suggested Fix
Unify the increment in a single place: drop the inner `.catch { retryCount++ }`; let the failure propagate out of `coroutineScope` and rely solely on `onFailure`:

```kotlin
runSuspendCatching {
    coroutineScope {
        info { "Subscribe to websocket" }
        block().collect {
            retryCount = 0
            verbose { "Receive changes by websocket: $it" }
            emit(it)
        }
    }
}.onFailure { e ->
    retryCount++
    error(e) { "Failed request websocket" }
}
```
