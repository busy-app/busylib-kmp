# `MutexSingleJobCoroutineScope.activeJobs` grows unboundedly

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/SingleJobCoroutineScope.kt` lines 80, 97, 111, 128

## Summary
Each call to `cancelPreviousUnsafe` / `awaitPreviousUnsafe` / `trySkipPreviousUnsafe` adds the freshly created `Deferred` to `activeJobs` via `also(activeJobs::add)`, but **nothing ever removes finished or cancelled jobs from the list**. The list grows linearly with the number of times `launch`/`async` is called over the lifetime of the scope.

## Repro
```kotlin
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default).asSingleJobScope()
repeat(1_000_000) {
    scope.launch(SingleJobMode.CANCEL_PREVIOUS) { /* immediate */ }.join()
}
// activeJobs now contains ~1_000_000 completed Deferred references
```

## Root Cause
- `cancelPreviousUnsafe` cancels every `activeJobs.forEach(Job::cancel)`, but cancelled jobs **stay in the list**.
- `awaitPreviousUnsafe` snapshots `activeJobs.toList()` for the new coroutine to await, but never removes joined jobs.
- `trySkipPreviousUnsafe` reads `activeJobs.any(Job::isActive)`. The check is correct but the list still grows.

## Impact
- Memory leak proportional to the rate of launches (BLE / WS / RPC re-subscription churn). A long-lived scope used as the central RPC scope will retain references to every `Deferred` ever created on it.
- `cancelPreviousUnsafe` performs an O(N) `forEach(Job::cancel)` over an ever-growing list. At 1 M past jobs, every new `launch` walks 1 M references inside the mutex, blocking other launchers.
- `awaitPreviousUnsafe` snapshots O(N) list and `joinAll` over already-completed jobs. Still O(N) inside the mutex.

## Suggested Fix
Either:
1. Track only the **last** active job (`var activeJob: Job?`) — this matches the documented "single job" semantics.
2. Or `invokeOnCompletion { activeJobs.remove(this) }` on each newly added job to garbage-collect finished entries.

Option (1) is preferable; the existing `activeJobs: MutableList<Job>` design has no behavioural advantage over a single reference and exists only to defer cleanup.
