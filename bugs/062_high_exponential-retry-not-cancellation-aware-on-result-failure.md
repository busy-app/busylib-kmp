# `exponentialRetry` retries indefinitely when `block` returns `Result.failure(CancellationException)`

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/RetryKtx.kt` lines 22–42

## Summary
`exponentialRetry` collects from `flow { emit(block.invoke().getOrThrow()) }.retry(retries) { … true }`. If `block` returns a `Result.failure(CancellationException)` — easy to do via `runCatching { … }` outside this function or by marshalling errors over a wire boundary — `Result.getOrThrow()` rethrows the wrapped CE inside the flow. The retry predicate **always returns `true`** without checking the throwable, so the CE is treated as a normal failure and the loop retries forever.

```kotlin
flow { emit(block.invoke().getOrThrow()) }
    .retry(retries) {
        val currentDelay = getExponentialDelay(retryCount = count++, …)
        delay(currentDelay)
        return@retry true                  // unconditional
    }
    .first()
```

## Repro
```kotlin
val canceller = launch {
    exponentialRetry(retries = 1000) {
        Result.failure(CancellationException("timeout"))
    }
}
delay(60_000) // canceller never reaches its body's success branch
canceller.cancel()
```
The retry loop will keep waking every `getExponentialDelay(...)` and re-invoking `block`, even though every attempt produces a CE, until external cancellation arrives. The CE is **caller-supplied** (not from coroutine cancellation), so `delay` does not bail out.

## Root Cause
1. `Result.failure(CancellationException)` is a legitimate Kotlin construct — `runCatching` (forbidden in this codebase, but external libraries do it) and JSON-RPC adapters can produce it.
2. The retry predicate ignores the throwable type. It should rethrow CE so the surrounding scope can decide whether to cancel.

`flow.retry` does **not** auto-special-case CE — it routes every throwable through the user predicate.

## Impact
- A single misbehaving wire layer that wraps a server "cancelled" reply in `Result.failure(CancellationException(...))` will pin a coroutine into an infinite retry loop, reaching the 10 s `maxDelay` and burning network forever.
- Combined with the `count: Int` overflow described in `bugs/high_retry-pow-overflow.md`, the delay floor eventually becomes 0, escalating to a CPU-bound spin.

## Suggested Fix
```kotlin
.retry(retries) { cause ->
    if (cause is CancellationException) throw cause
    val currentDelay = getExponentialDelay(retryCount = count++, …)
    delay(currentDelay)
    true
}
```
And/or unwrap the failure before `getOrThrow()`:
```kotlin
flow {
    val r = block.invoke()
    r.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    emit(r.getOrThrow())
}
```
