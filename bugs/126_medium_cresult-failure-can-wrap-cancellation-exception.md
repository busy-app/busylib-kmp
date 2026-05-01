# `CResult` and `Result.toCResult()` accept `CancellationException` as a normal failure, breaking cancellation propagation

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/wrapper/src/commonMain/kotlin/net/flipper/busylib/core/wrapper/CResult.kt` lines 22, 56, 65–70

## Summary
`CResult.Failure(error: Throwable)` and `CResult.failure(error)` accept *any* `Throwable`, including `kotlin.coroutines.cancellation.CancellationException`. Combined with `Result<T>.toCResult()` (which performs a `fold`), it is trivial to construct a `CResult` whose `Failure.error` is a `CancellationException`. Downstream code that calls `getOrThrow()` on such a `CResult` will throw `CancellationException` — but **outside** the suspending context that originated the cancellation, breaking structured concurrency.

```kotlin
fun <T> Result<T>.toCResult(): CResult<T> {
    return fold(
        onSuccess = { CResult.Success(it) },
        onFailure = { CResult.Failure(it) }   // includes CE
    )
}
```

`runSuspendCatching` correctly rethrows CE, but `Result<T>` produced by user code via `runCatching { ... }` (still used in third-party code despite this codebase forbidding it) can hold a CE — and `toCResult()` happily wraps it.

The Swift side, after a `CResult.Failure` is bridged, eventually calls `getOrThrow()` on a Kotlin-side suspend, which then throws CE on a coroutine that did **not** receive a real cancellation request. Result: a fresh exception path that is silently retried (see `bugs/high_exponential-retry-not-cancellation-aware-on-result-failure.md`), or that crashes the surrounding scope.

## Repro
```kotlin
val r: Result<Int> = runCatching {
    coroutineScope {
        cancel(CancellationException("server-side"))
        42
    }
}                                            // r is Result.failure(CancellationException)

val cr: CResult<Int> = r.toCResult()          // wraps CE
val v = cr.getOrThrow()                       // throws CE on a non-cancelled scope
```

## Root Cause
`CResult.Failure` does not distinguish "logical failure" from "structured-concurrency cancellation". `toCResult()` does not strip CE out of the input.

## Impact
- Cancellation can leak from one scope into another via `CResult`.
- `exponentialRetry` retries indefinitely (see related bug) on `CResult` failures whose error happens to be a CE.

## Suggested Fix
1. In `Result.toCResult()`, rethrow CE rather than wrapping it:
   ```kotlin
   fun <T> Result<T>.toCResult(): CResult<T> {
       val ex = exceptionOrNull()
       if (ex is CancellationException) throw ex
       return fold(onSuccess = { CResult.Success(it) }, onFailure = { CResult.Failure(it) })
   }
   ```
2. In `CResult.Failure`'s constructor, reject CE:
   ```kotlin
   data class Failure(val error: Throwable) : CResult<Nothing>() {
       init { check(error !is CancellationException) { "CResult must not wrap CancellationException" } }
   }
   ```
