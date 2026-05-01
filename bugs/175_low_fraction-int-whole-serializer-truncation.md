# `FractionIntWholeSerializer` truncates values, losing 0.99 vs 0.999 distinction across round-trips

## Type
broken-feature

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/data/src/commonMain/kotlin/net/flipper/core/busylib/data/serialization/FractionIntWholeSerializer.kt` lines 16–22

## Summary
The encoder writes `value.toWholePercent().toInt()`. `toWholePercent` returns `Double` in `[0.0, 100.0]`. `Double.toInt()` truncates toward zero. So `Fraction.fromFraction(0.999)` (`toWholePercent() == 99.9`) serialises as `99` and deserialises back to `Fraction.fromWholePercent(99)` = `0.99`.

```kotlin
override fun serialize(encoder: Encoder, value: Fraction) {
    encoder.encodeInt(value.toWholePercent().toInt())   // truncate toward 0
}
```

This is a documented data loss for any `Fraction` whose decimal expansion has more than 2 digits.

## Root Cause
Truncation. `roundToInt()` would at least round to the nearest integer; an explicit choice was made to discard precision.

## Impact
- A round-trip `Fraction(0.501) → 50 → Fraction(0.50)` loses 1% of value.
- For brightness, volume, and progress fractions exchanged with the BUSY Bar, this is the wire-protocol resolution; truncation matches the protocol's intent.
- For other consumers that use `FractionIntWholeSerializer` in their own JSON, accuracy may be lower than expected.

## Suggested Fix
Either document that this serialiser is lossy by design, or use rounding:
```kotlin
encoder.encodeInt(value.toWholePercent().roundToInt())
```
