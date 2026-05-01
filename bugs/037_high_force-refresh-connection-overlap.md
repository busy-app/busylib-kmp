# `forceRefreshConnection` may run two `connectionLoop`s concurrently

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/service/impl/src/commonMain/kotlin/net/flipper/bridge/connection/service/impl/FConnectionServiceImpl.kt` lines 47-104
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/SingleJobCoroutineScope.kt` lines 100-112, 153-195 (root cause is in core:ktx but its symptom surfaces here)

## Summary

`FConnectionServiceImpl` runs `connectionLoop()` under
`singletonScope.launch(SingleJobMode.SKIP_IF_RUNNING) { ... }` from
`onLaunch()`, and re-launches it under
`singletonScope.launch(SingleJobMode.CANCEL_PREVIOUS) { ... }` from
`forceRefreshConnection()`.

`MutexSingleJobCoroutineScope.cancelPreviousUnsafe` does not await the
cancellation of the previous job — it just calls `Job::cancel` on each entry
in `activeJobs` and immediately starts a new one:

```kotlin
private fun <T> cancelPreviousUnsafe(...): Deferred<T> {
    activeJobs.forEach(Job::cancel)            // fire-and-forget cancel
    return scope.async(... block = block ...)  // new coroutine starts NOW
        .also(activeJobs::add)
}
```

`Job::cancel` is non-suspending; the previous loop continues to execute until
its next cancellation check. Because `combine { ... when (realState) {...} }`
calls `orchestrator.connectIfNot(...)` / `orchestrator.disconnectCurrent()`
inside the transform without an explicit cancellation check, the old loop can
fire one more side effect after the new loop has already started, while the
new loop is concurrently issuing its own commands.

The cumulative effect is: `forceRefreshConnection()` may issue
`disconnectCurrent` from the new loop *and* `connectIfNot` from the old
loop's tail, racing on `orchestrator`'s mutex. The order of mutex acquisition
is not stable, so the post-`forceRefresh` state becomes nondeterministic
("connecting" vs. "disconnected").

## Repro

1. User triggers `forceRefreshConnection()` while the existing connection
   loop is suspended inside `orchestrator.connectIfNot(...)` (i.e. waiting
   on the orchestrator's `mutex`).
2. New loop launches; old loop is marked cancelled but is still parked on the
   mutex. Cancellation only takes effect when the mutex returns control —
   `Mutex.withLock` checks for cancellation on resume.
3. New loop calls `orchestrator.disconnectCurrent()`, acquires the mutex,
   disconnects, releases mutex.
4. Old loop resumes from `withLock`, sees cancellation, exits — but its
   `combine` transform may have already invoked another effect after
   `connectIfNot` returned.
5. Net behaviour: connect command appears to "win" intermittently, leaving
   the device connected even though the user requested a forced refresh that
   should culminate in a disconnect-then-reconnect.

## Root Cause

`SingleJobMode.CANCEL_PREVIOUS` semantics in `MutexSingleJobCoroutineScope`
do not include "wait for the previous job to actually finish before starting
the next". Watchers and the connection service rely on the stronger
"strict serial" semantic. The `connectionLoop` itself does not poll
`isActive`/`ensureActive` between branches.

A secondary issue in the same scope: `activeJobs` grows unbounded — every
launched job is appended via `also(activeJobs::add)` and never removed,
even after completion. Memory grows linearly with the number of
`forceRefreshConnection`/feature-restart events.

## Impact

- After `forceRefreshConnection`, the device may end up connected with the
  *previous* state instead of the freshly requested one.
- Latent memory growth in long-lived sessions (every watcher uses
  `asSingleJobScope()` and most reschedule on reconnect).

## Suggested Fix

1. In `MutexSingleJobCoroutineScope.cancelPreviousUnsafe`, await
   `previousJobs.joinAll()` (after `cancel`) before launching the new job.
   This makes `CANCEL_PREVIOUS` strictly serial and matches naive intuition.
2. Prune `activeJobs` of completed entries — easiest is to register
   `invokeOnCompletion { activeJobs.remove(this) }` on every launched job
   (under `mutex`).
3. Add `ensureActive()` between branches in `connectionLoop`'s `combine`
   transform to make cancellation latency-bounded even without (1).
