# `DefaultObjectCache.getEntry` writes `Pending` into the cache as a side-effect of any read

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/cache/DefaultObjectCache.kt` lines 72–83

## Summary
`getEntry(clazz)` performs `cache.getOrPut(clazz) { CacheEntry.Pending }`. This means any code path that calls `getEntry` for a missing key writes `Pending` into the cache, even when the caller was about to immediately overwrite it via `putEntry`. The `Pending` value carries no information that any reader uses — every consumer of `getEntry`'s result checks `entry as? CacheEntry.Created<*>` and discards the `Pending` case via `?:` fallthrough.

```kotlin
private fun getEntry(clazz: KClass<*>): CacheEntry<*> {
    val entry = cache.getOrPut(clazz) { CacheEntry.Pending }     // mutates cache
    val newEntry = when (entry) {
        is CacheEntry.Created<*> -> entry.copy(lastReadAt = timeProvider.markNow())
        CacheEntry.Pending -> entry
    }
    cache[clazz] = newEntry                                       // re-writes
    return newEntry
}
```

Consequences:
1. The mutation has been described as a separate leak in `bugs/high_default-object-cache-pending-leak.md`.
2. Even on the happy path, the cache momentarily holds a `Pending` entry. If a second concurrent caller observes that `Pending` between `getEntry`'s `getOrPut` and `putEntry`'s overwrite, it correctly falls through to `putEntry` again — but in practice this can never happen because the entire body is inside `mutex.withLock`. So `Pending` is a *useless* sentinel.
3. The function name `getEntry` implies a pure read; it is actually a read+write.

## Root Cause
The `Pending` state was introduced for a use case that no longer exists — there is no path that distinguishes "no entry" from "pending entry being computed by another caller". `getOrElse` always recomputes if `entry !is Created`, so `Pending` is indistinguishable from absence.

## Impact
- Mild memory pressure (one map slot per unique cache key ever queried, even for failed/cancelled requests).
- Code smell: side-effecting `getEntry` makes the algorithm harder to reason about.

## Suggested Fix
Either drop the `Pending` sentinel entirely, or make `getEntry` a pure read that returns `null` for absent entries. See `bugs/high_default-object-cache-pending-leak.md` for the suggested implementation.
