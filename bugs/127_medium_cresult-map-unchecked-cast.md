# `CResult.map` performs an unchecked cast through a star-projected `Success<*>`

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/wrapper/src/commonMain/kotlin/net/flipper/busylib/core/wrapper/CResult.kt` lines 72–77

## Summary
```kotlin
inline fun <R, T> CResult<T>.map(transform: (value: T) -> R): CResult<R> {
    return when (this) {
        is CResult.Success<*> -> CResult.success(transform(this.value as T))
        is CResult.Failure -> CResult.Failure(this.error)
    }
}
```

The `is CResult.Success<*>` check is **less specific** than `is CResult.Success<T>`, but Kotlin cannot smart-cast generics through type erasure. The author works around this with `this.value as T`, an unchecked cast suppressed only by the implicit `T` annotation. With `inline fun`, the cast site is the call site, so the cast inherits the *call-site* `T`. If a Swift caller invokes a Kotlin function returning `CResult<String>` and at the bridge the type token is wrong (which can happen with SKIE's enum-with-associated-values bridging), the cast can succeed silently and produce a `String` posing as `Int` until first use.

A better-behaved variant uses smart-cast `is CResult.Success<T>`:

```kotlin
inline fun <R, T> CResult<T>.map(transform: (value: T) -> R): CResult<R> = when (this) {
    is CResult.Success -> CResult.success(transform(this.value))
    is CResult.Failure -> CResult.Failure(this.error)
}
```

`is CResult.Success` (no projection) here lets Kotlin smart-cast `value` to `T` because `Success<T> : CResult<T>`.

## Root Cause
- The author wrote `is CResult.Success<*>` rather than `is CResult.Success` and then forced the type via `as T`.
- This is a leftover from earlier code; there is no semantic reason for it.

## Impact
- The unchecked cast can silently route a wrongly-typed `Success` through `map` — particularly relevant for the Swift-bridged path that produces `CResult` instances at the boundary.
- Loss of compile-time guarantees that `map` itself is what AGENTS.md describes as the canonical Result-mapping abstraction.

## Suggested Fix
Replace the unchecked cast with smart-casting:
```kotlin
inline fun <R, T> CResult<T>.map(transform: (value: T) -> R): CResult<R> = when (this) {
    is CResult.Success -> CResult.success(transform(this.value))
    is CResult.Failure -> CResult.Failure(this.error)
}
```
