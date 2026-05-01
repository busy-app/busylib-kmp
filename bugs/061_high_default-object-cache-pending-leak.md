# `DefaultObjectCache` leaks `CacheEntry.Pending` entries forever

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/cache/DefaultObjectCache.kt` lines 47–52, 72–83, 85–106

## Summary
`getEntry` calls `cache.getOrPut(clazz) { CacheEntry.Pending }`. If a caller passes a `clazz` that has never been cached and the `block`'s `async` is never started (e.g. parent scope cancelled before `putEntry`, or `getOrElse` itself cancelled before reaching `putEntry`), a `Pending` sentinel remains in `cache` indefinitely.

`clearExpiredUnsafe` filters by `(value as? CacheEntry.Created<*>?)?.isExpired == true`. `Pending` is never `Created`, so the filter never matches it; the entry survives every cleanup pass.

## Repro
1. Concurrently call `cache.getOrElse(ignoreCache=false, KClass<X>) { … long suspend block … }` and cancel the outer coroutine before the mutex is acquired.
2. Inspect the internal `cache` map (or pin a heap dump) — `KClass<X> -> CacheEntry.Pending` survives `clearExpiredUnsafe` indefinitely.

While this exact path is a narrow window, a similar leak occurs whenever the `async { block.invoke() }` is created and then immediately cancelled — the `Created` entry's `lastReadAt` stays current (because nothing re-reads it), so `isExpired` is false until the read-timeout fires; but the leak above (`Pending` left over) is permanent.

## Root Cause
- `getEntry` writes `Pending` as a sentinel but the value is never consumed: every caller checks `entry as? CacheEntry.Created<*>` and falls through to `putEntry`. The `Pending` write is dead code that has only side-effects (memory residency).
- `clearExpiredUnsafe` does not prune `Pending`.

## Impact
- Slow accumulation of `KClass -> Pending` mappings across unique cache types invoked under cancellation.
- For long-lived `DefaultObjectCache` instances (per-app singleton in DI), cumulative leak proportional to distinct cache keys.

## Suggested Fix
- Drop the `getOrPut(clazz) { CacheEntry.Pending }` write (the simple `cache[clazz]` read is enough, since the only branch that uses it falls through to `putEntry`):

```kotlin
private fun getEntry(clazz: KClass<*>): CacheEntry<*>? {
    val entry = cache[clazz] ?: return null
    if (entry is CacheEntry.Created<*>) {
        cache[clazz] = entry.copy(lastReadAt = timeProvider.markNow())
    }
    return entry
}
```
- Or have `clearExpiredUnsafe` also prune any `Pending` whose containing async is no longer active.
