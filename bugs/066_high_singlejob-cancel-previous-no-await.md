# `cancelPreviousUnsafe` does not wait for previous job to actually finish before launching the new one

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/SingleJobCoroutineScope.kt` lines 100–112

## Summary
`SingleJobMode.CANCEL_PREVIOUS` calls `activeJobs.forEach(Job::cancel)` and immediately starts a new `scope.async { block }`. `Job.cancel()` only **requests** cancellation — the previous coroutine may continue executing for an arbitrary amount of time before observing the cancellation point. Both the old and new bodies can therefore run concurrently on the same `SingleJobCoroutineScope`, breaking the "only one job at a time" guarantee that the abstraction promises.

```kotlin
private fun <T> cancelPreviousUnsafe(...): Deferred<T> {
    activeJobs.forEach(Job::cancel)            // request only
    return scope.async(...).also(activeJobs::add) // starts immediately
}
```

## Repro
```kotlin
val scope = CoroutineScope(Dispatchers.Default).asSingleJobScope()
val state = mutableListOf<Int>()
val mutex = Mutex()

scope.launch(SingleJobMode.CANCEL_PREVIOUS) {
    // A hot, non-cancellable section
    repeat(1_000_000) {
        state.add(0) // does not check cancellation
    }
}
delay(1)
scope.launch(SingleJobMode.CANCEL_PREVIOUS) {
    // Both bodies are racing on `state`, despite SingleJobMode.CANCEL_PREVIOUS
    state.add(1)
}.join()
```
Result: data race / `ConcurrentModificationException` / inconsistent state.

## Root Cause
The implementation treats `cancel()` as if it were synchronous, but Kotlin coroutine cancellation is cooperative. The previous job may be:
- inside a `withContext(NonCancellable)` block,
- in a CPU-bound loop without `yield`/`isActive` check,
- inside a JNI / native call.

In all of these cases the previous body keeps running while the new body has already started.

## Impact
- Race conditions on any state shared across `CANCEL_PREVIOUS` invocations (BLE callbacks, RPC channels, mutable maps, etc.).
- Particularly dangerous for the BLE feature pipeline that calls `singleJobScope.launch(CANCEL_PREVIOUS)` to "switch" to a new device — both old-device and new-device handlers can run together.

## Suggested Fix
Use `cancelAndJoin` semantics:

```kotlin
private suspend fun <T> cancelPreviousUnsafe(...): Deferred<T> {
    val previous = activeJobs.toList()
    previous.forEach(Job::cancel)
    previous.joinAll()                       // wait until each really finishes
    return scope.async(context, start, block).also(activeJobs::add)
}
```

This guarantees the previous body has executed its `finally` blocks before the new body starts, satisfying the contract advertised by `SingleJobMode`.
