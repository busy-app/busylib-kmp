# `DefaultHttpRequestThrottler` holds the mutex during `delay()`, fully serialising RPC

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/feature/rpc/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/rpc/impl/util/throttle/DefaultHttpRequestThrottler.kt` (lines 35–56)
- `components/bridge/feature/rpc/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/rpc/impl/util/HttpClientFactory.kt` (lines 19, 44–49) — configured at `MAX_RPS = 2`, `refillPeriod = 1.s`

## Summary
The token-bucket throttler used by every RPC client wraps both the bucket maths and the rate-limit `delay(...)` inside a single `mutex.withLock { ... }`. When the bucket is empty, every subsequent caller queues behind the mutex, so they don't even get to evaluate whether the bucket has refilled — they just wait for the previous holder's full delay. Effective concurrency under burst is exactly 1, regardless of how many tokens-per-window the server actually allows.

## Repro
1. Issue 10 concurrent RPC calls (e.g. `getWifiStatus`, `getName`, `getDisplayBrightness`, etc.) at t=0.
2. With `MAX_RPS=2, refillPeriod=1.s`, expected throughput: 2/sec → 10 reqs in ~5s.
3. Actual: each request that finds `remaining<=0` takes the mutex, computes `delay = reset - now` (~1s) and **awaits inside the lock**. The next caller cannot even check the bucket until the previous one finishes its delay. With many waiters, the total observed time becomes `(n / limit) * refillPeriod`, but **strictly serialized through the mutex**, blocking even the responses (since `Send` plugin is single-pass).

```kotlin
mutex.withLock {
    if (refillPeriod > Duration.ZERO) {
        if (remaining <= 0) {
            val delay = reset - Clock.System.now()
            delay(delay)                       // <-- inside the lock
            reset = Clock.System.now() + refillPeriod
            remaining = limit
        }
        remaining--
    }
}
```

## Root Cause
- The implementation was lifted verbatim (per the `@author` comment) from a 3rd-party reference. The reference code holds the lock during the delay, which prevents bucket re-evaluation while waiting and creates head-of-line blocking.
- All RPC requests on a single device share one `HttpClient`, hence one throttler instance, so the bottleneck is per-device global.

## Impact
- Under any burst (e.g. opening a screen that calls `getStatusFirmware`, `getStatusSystem`, `getName`, `getAudioVolume`, `getDisplayBrightness`, `getMatterCommissioning`, `getBleStatus` at once), all calls are funnelled one-at-a-time even though the device tolerates 2/s.
- Retries amplified by `exponentialRetry` (used in many feature flows) further pile up behind the lock, multiplying perceived latency.
- Visible UX: lists of features load sequentially; first paint takes seconds longer than necessary.
- Worse, if `delay(delay)` is cancelled (e.g. caller scope dies), the lock is released without `remaining` being updated — followers re-check `remaining <= 0` and pay the delay again.

## Suggested Fix
- Compute the delay *under the lock*, then release the lock and `delay(...)` outside it. Re-enter the lock to update bucket state. Sketch:
  ```kotlin
  while (true) {
      val toDelay: Duration = mutex.withLock {
          if (refillPeriod <= Duration.ZERO) return@withLock Duration.ZERO
          if (remaining > 0) { remaining--; return@withLock Duration.ZERO }
          // bucket empty
          val now = Clock.System.now()
          if (now >= reset) {
              reset = now + refillPeriod; remaining = limit - 1
              return@withLock Duration.ZERO
          }
          reset - now
      }
      if (toDelay <= Duration.ZERO) return
      delay(toDelay)
  }
  ```
- Add a unit test that fires N concurrent `throttle()` calls and asserts they finish in ~`(N / limit) * refillPeriod`, not `N * refillPeriod`.
