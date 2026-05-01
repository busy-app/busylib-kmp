# `SingleJobCoroutineScope.listenForOnDestroy` calls `runBlocking` from `invokeOnCompletion`

## Type
infrastructure

**Severity:** critical

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/SingleJobCoroutineScope.kt` lines 197–203, called from `init` (line 205–207)

## Summary
`MutexSingleJobCoroutineScope.listenForOnDestroy()` registers an `invokeOnCompletion` handler on the parent scope's `Job`. The handler executes `runBlocking { mutex.withLock { activeJobs.clear() } }` synchronously on the thread that finished the parent job — blocking that thread on a coroutine `Mutex`.

```kotlin
private fun listenForOnDestroy() {
    scope.coroutineContext[Job]?.invokeOnCompletion {
        runBlocking {
            mutex.withLock { activeJobs.clear() }
        }
    }
}
```

## Repro
1. Create a `SingleJobCoroutineScope` whose parent `Job` is part of an iOS / macOS / Android Main scope.
2. Have a coroutine inside the scope holding the mutex when the parent is cancelled (e.g. coroutine started a `CANCEL_PREVIOUS` job and is in `mutex.withLock { ... }`'s body).
3. Cancel the parent scope. The cancellation runs `invokeOnCompletion` on the cancelling thread (often Main/UI).
4. `runBlocking` on Main with a held mutex deadlocks the dispatcher.

## Root Cause
`invokeOnCompletion` callbacks must be **fast and non-blocking** — they run on whatever thread completes the job. `runBlocking` cannot acquire the dispatcher it is invoked on (Main/Default), and it can deadlock if the held mutex was acquired by a coroutine that needs the same dispatcher to release.

Additionally, the call serves no real purpose: when the parent `Job` completes, all child coroutines and their jobs are already cancelled, so clearing `activeJobs` has no effect on coroutine lifecycles — only on the GC root path of completed `Job` objects.

## Impact
- Deadlock of Main / UI thread on any iOS / macOS app that drives `SingleJobCoroutineScope` from the Main dispatcher and cancels at the wrong instant.
- ANR / app hang on Android.
- The bug is silent: tests with `runTest` schedule everything on a single dispatcher, so `runBlocking` never deadlocks there.

## Suggested Fix
Drop the entire `listenForOnDestroy` mechanism, or make it non-blocking:

```kotlin
private fun listenForOnDestroy() {
    scope.coroutineContext[Job]?.invokeOnCompletion {
        // synchronous, non-suspending: list is already orphan, no need for the mutex
        activeJobs.clear()
    }
}
```

The `mutex.withLock` is meaningless here — once the parent is dead, no other coroutine can acquire the mutex. Just clear the list synchronously, or drop the call entirely.
