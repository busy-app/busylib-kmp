# `Flow.throttleLatestCached` reads-then-writes `previous` from a `launch` coroutine — race on rapid emissions

## Type
infrastructure

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/DebounceKtx.kt` lines 34–49

## Summary
```kotlin
fun <T, K> Flow<T>.throttleLatestCached(
    transform: suspend (T, K?) -> K
): Flow<K> = channelFlow {
    val scope = this
    var job: Job? = null
    var previous: K? = null

    collectLatest { value ->
        job?.join()
        job = scope.launch {
            val current = transform.invoke(value, previous)
            send(current)
            previous = current
        }
    }
}
```

`previous` is captured by reference inside `launch { … }`. The outer `collectLatest` may proceed to its next iteration after `job?.join()` succeeds, but **inside** `launch` the read of `previous` (passed into `transform.invoke(value, previous)`) and the write `previous = current` happen on a different coroutine — `launch` is dispatched. If the dispatcher is multi-threaded (`Dispatchers.Default`), the next `collectLatest { … }` iteration can read `previous` *before* the previous launch wrote `current` to it (because `job?.join()` only waits for the previous launched body to *complete*, but a strict happens-before relationship between `previous = current` and the next iteration's *read* depends on the dispatcher).

Actually: `Job.join()` provides a happens-before edge: everything before `complete()` in the joined coroutine happens-before everything after `join()` returns. The previous `launch` body's last statement is `previous = current`, which happens-before the next iteration's `transform.invoke(value, previous)` *if and only if* `previous` is volatile / atomic / accessed via memory fence. In Kotlin/JVM, `Job.join()` does establish happens-before via the underlying coroutine machinery (CompletableJob's internal state uses CAS), so on JVM this is safe.

On Kotlin/Native — the new memory model also guarantees happens-before through `join()` in the documented `kotlinx.coroutines` contract. So functionally OK.

The real bug is more subtle: `collectLatest { value -> job?.join() }` cancels `block`'s previous body when a new value arrives, but the `launch { … }` inside is a *child* of `collectLatest`'s body. Cancellation of the outer body cancels the inner `launch`. The `job?.join()` then returns due to cancellation. Because of `cancelAndJoin`'s semantics, `previous = current` may **not** have been written if the launched body was cancelled mid-`transform.invoke()`. The next iteration reads stale `previous`.

In other words: under high emission rate, `transformLatestCached` may invoke `transform(value_n, previous=value_{n-2})` instead of `previous=value_{n-1}`, because the n-1 job got cancelled before writing.

## Repro
1. Emit 100 values per second to a `throttleLatestCached` whose `transform` takes 50 ms.
2. About half the values get cancelled mid-flight; their `previous = current` write is skipped.
3. Downstream sees `previous` lagging behind by 1–2 values across the burst.

## Root Cause
- `previous` is updated only on successful completion of the launched body. Cancellation skips the write.
- `collectLatest` cancels in-flight bodies, breaking the invariant "previous == last successfully transformed value".

## Impact
- Mostly cosmetic: caches may be slightly out of date.
- For consumers who use `previous` as a delta marker (e.g. "compute diff from the last emission"), this can produce wrong diffs.

## Suggested Fix
Use `try/finally` to ensure `previous` is always updated to the **input** value, or use `collectLatest`'s contract more carefully:

```kotlin
fun <T, K> Flow<T>.throttleLatestCached(
    transform: suspend (T, K?) -> K
): Flow<K> = channelFlow {
    var previous: K? = null
    collect { value ->
        // sequential transform — no cancellation between read and write
        val current = transform(value, previous)
        send(current)
        previous = current
    }
}
```
This loses the "throttle" part — the original intent was to drop intermediate values. If that is required, track `previous` based on the *last input* (not last successful output) and protect the read/write under a mutex.
