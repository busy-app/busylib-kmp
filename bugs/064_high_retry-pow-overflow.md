# `getExponentialDelay` uses `factor.pow(retryCount)` which overflows for large counts and is not robust to `Long.MAX_VALUE` retries

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/RetryKtx.kt` lines 11–20, 22–42

## Summary
`getExponentialDelay` computes `initialDelay * factor.pow(retryCount)`. For `factor = 2.0` and `retryCount ≥ 1024` this becomes `Double.POSITIVE_INFINITY`. `Duration * Double.POSITIVE_INFINITY` is `Duration.INFINITE`, and although `coerceAtMost(maxDelay)` saves the day in the *value*, intermediate `Duration` arithmetic on `Double.POSITIVE_INFINITY * Duration` is undefined for negative inputs and produces NaN if `retryCount` ever becomes negative (e.g. through wrap-around).

```kotlin
val resultDelay = initialDelay * factor.pow(retryCount)
return resultDelay.coerceAtMost(maxDelay)
```

`exponentialRetry` uses `var count = 0` increased on every retry without bounds, called by `wrapWebsocket` with `retries = Long.MAX_VALUE`. `count: Int` overflows to `Int.MIN_VALUE` after `Int.MAX_VALUE` retries → `factor.pow(Int.MIN_VALUE) = 0.0` → `delay(0)` infinite loop, hammering the network.

## Repro
- For overflow: synthetic `getExponentialDelay(retryCount = 2_000_000_000)` → `Duration.INFINITE` → coerced to `maxDelay` (correct), but…
- `getExponentialDelay(retryCount = -1)` → `factor.pow(-1) = 0.5` → `initialDelay * 0.5` = `500ms`, smaller than `initialDelay`. The function does not enforce `retryCount >= 0`.
- `exponentialRetry` runs ≥ `Int.MAX_VALUE` retries (long-lived background WS) → `count++` wraps to `Int.MIN_VALUE` → `factor.pow(Int.MIN_VALUE)` is `0.0` → no delay → tight reconnect loop until process death.

## Root Cause
- `count` is `Int` but `retries` is `Long.MAX_VALUE`.
- `factor.pow(retryCount)` is unbounded numerically.
- No `coerceAtLeast(0)` on the exponent.

## Impact
- After an extremely long-running session, the back-off becomes effectively zero, producing a busy-loop reconnect storm — exactly the failure mode `exponentialRetry` is meant to prevent.
- In `wrapWebsocket` (which feeds `getExponentialDelay(retryCount)` directly, never bounded), the same exponent-overflow path applies.

## Suggested Fix
1. Cap the exponent before passing to `pow`:
   ```kotlin
   val safeRetry = retryCount.coerceIn(0, 32) // 2^32 ≫ 10s cap
   val resultDelay = initialDelay * factor.pow(safeRetry)
   ```
2. Use `Long` for the counter inside `exponentialRetry` (and stop sharing `Int` with `Math.pow`'s `Double` exponent — use `toDouble().coerceAtMost(64.0)`).
3. Reset / saturate `retryCount` once `resultDelay >= maxDelay` to avoid further unnecessary work.
