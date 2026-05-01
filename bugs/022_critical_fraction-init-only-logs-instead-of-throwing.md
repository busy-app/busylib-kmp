# `Fraction.init` only logs and continues on out-of-range values, allowing invalid `Fraction` instances

## Type
broken-feature

**Severity:** critical

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/data/src/commonMain/kotlin/net/flipper/core/busylib/data/Fraction.kt` lines 40–44

## Summary
The `Fraction` data class advertises (via its KDoc) that it "enforce[s] valid bounds at construction time" and prevents "invalid values like 1.5 or -0.2". However, the `init` block uses **the logging `error { ... }` from `net.flipper.core.busylib.log`**, not the stdlib `kotlin.error()` (which throws `IllegalStateException`):

```kotlin
init {
    if (value !in MIN_FRACTION..MAX_FRACTION) {
        error { "#init Fraction not in range 0.0..1.0, got $value" }   // imports net.flipper.core.busylib.log.error
    }
}
```

Because `LogTagProvider.error` simply emits a log line and returns `Unit`, an invalid `Fraction` is constructed silently. `Fraction.fromFraction(2.0)` succeeds, `toWholePercent()` later produces `200.0` (then coerced) and `toDouble()` returns `2.0`.

## Repro
```kotlin
val bad = Fraction.fromFraction(5.0)
println(bad.toDouble())            // 5.0   ← contract says ≤ 1.0
println(bad.toWholePercent())      // 100.0 (coerced) — silently masks the bug
println(bad.toFloat())             // 5.0f  ← still invalid
```

## Root Cause
Wrong `error` import. `net.flipper.core.busylib.data.Fraction.kt` imports `net.flipper.core.busylib.log.error`, which is a logging function with signature `inline fun error(logMessage: () -> String)`. The author likely meant `kotlin.error("…")` which throws.

## Impact
- The class invariant (`0.0 ≤ value ≤ 1.0`) is not enforced; downstream code that assumes a valid range can produce out-of-range hardware commands (LED brightness > 100 %, animation progress out of bounds, etc.).
- `FractionIntWholeSerializer` writes `value.toWholePercent().toInt()` — for an invalid `Fraction(5.0)` this is `100` (coerced inside `toWholePercent`), so wire serialisation hides the underlying bug while in-memory consumers still see `5.0`.
- A bug that should be a fast crash instead becomes silent corruption.

## Suggested Fix
Use `require` (or stdlib `error`) so the constructor throws when invariants are violated:

```kotlin
init {
    require(value in MIN_FRACTION..MAX_FRACTION) {
        "Fraction must be in [0.0, 1.0], got $value"
    }
}
```

Equivalent:
```kotlin
init {
    if (value !in MIN_FRACTION..MAX_FRACTION) {
        kotlin.error("Fraction not in range 0.0..1.0, got $value")
    }
}
```
