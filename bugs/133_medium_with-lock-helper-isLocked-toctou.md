# `LogTagProvider.withLock(...)`'s `mutex.isLocked` check is racy & misleading

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/CoroutineLogKtx.kt` lines 31–49

## Summary
The helper logs an "I can't execute right now" line if the mutex is currently locked, *before* attempting `mutex.withLock`:

```kotlin
suspend fun <T> LogTagProvider.withLockResult(
    mutex: Mutex,
    tag: String? = null,
    action: suspend () -> T
): T {
    if (mutex.isLocked) {
        info { "I can't execute right now job $tag because $mutex is locked" }
    }
    return mutex.withLock { … action() … }
}
```

Two issues:
1. **TOCTOU race**: `isLocked` is sampled outside any synchronization. By the time we call `withLock`, the mutex may have been released (so the log message is misleading). Conversely, a previously unlocked mutex may now be locked (so the log message is missing).
2. **Wording is wrong**: the message says "I can't execute right now" but actually the call *will* execute as soon as the mutex frees. Readers are misled into believing the call returned without running the block.

The implementation also wraps the action in the mutex but does not log time-spent waiting for the mutex (only the mutex hold time, via `startTime`). For diagnostics this is the wrong measurement.

## Repro
- Start two concurrent calls to `withLock(mutex, "alpha")`. Whether the second one logs depends entirely on scheduling.

## Root Cause
- `Mutex.isLocked` is a snapshot-of-state property without ordering guarantees.
- The log message confuses queueing with rejection.

## Impact
- Misleading logs for engineers diagnosing contention.
- Time spent waiting for the lock is not captured, so contention cannot be attributed to the right caller.

## Suggested Fix
Either drop the pre-check and capture wait time inside the lock:

```kotlin
suspend fun <T> LogTagProvider.withLockResult(
    mutex: Mutex,
    tag: String? = null,
    action: suspend () -> T
): T {
    val requestedAt = Clock.System.now().toEpochMilliseconds()
    return mutex.withLock {
        val acquiredAt = Clock.System.now().toEpochMilliseconds()
        val waited = acquiredAt - requestedAt
        if (waited > WARN_MUTEX_WAIT_MILLIS) {
            warn { "Waited ${waited}ms to acquire $mutex for tag=$tag" }
        }
        verbose { "Launch $tag job in mutex mode after ${waited}ms wait" }
        try { action() }
        finally { verbose { "Completed $tag in ${Clock.System.now().toEpochMilliseconds() - acquiredAt}ms" } }
    }
}
```
This both eliminates the race and properly records contention.
