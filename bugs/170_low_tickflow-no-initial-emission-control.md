# `TickFlow` may emit immediately if `initialDelay` is zero — undocumented, surprising for time-based subscribers

## Type
infrastructure

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/TickFlow.kt`

## Summary
```kotlin
class TickFlow(
    duration: Duration,
    initialDelay: Duration = Duration.ZERO
) : Flow<Unit> by flow(
    block = {
        delay(initialDelay)
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(duration)
        }
    }
)
```

By default, `initialDelay = Duration.ZERO`, so subscribers receive an emission *synchronously on subscribe*. Many callers expect a "tick every N seconds" semantics where the first tick is **after** `duration`. The default of `Duration.ZERO` violates that expectation. The KDoc claims only "is used to tick every [duration]" — silent on the first tick.

If `duration` is `Duration.ZERO` and `initialDelay` is `Duration.ZERO`, the loop becomes `while (isActive) { emit; delay(0) }` — a tight, dispatcher-saturating loop. `delay(Duration.ZERO)` does suspend (and yields), so the tightness is bounded by dispatcher fairness, but on a single-thread dispatcher this can starve other coroutines.

## Repro
```kotlin
TickFlow(Duration.ZERO).take(10).collect { println(it) }
// Possibly starves other coroutines on the same dispatcher
```

## Root Cause
- No validation of `duration > 0`.
- Default `initialDelay = Duration.ZERO` is surprising.

## Impact
- Surprise initial emission for callers expecting periodic ticks.
- Potential dispatcher starvation if `duration == 0`.

## Suggested Fix
1. Default `initialDelay = duration` so first tick is **after** the period (matching most "tick every N" expectations).
2. `require(duration > Duration.ZERO) { "TickFlow requires positive duration, got $duration" }`.
3. Document the first-tick semantics in KDoc.
