# `MutexConsumable` reads `isConsumed` outside the mutex causing a race on Kotlin/Native and a benign one on JVM

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/Consumable.kt` lines 78–96

## Summary
`MutexConsumable.tryConsume` performs a read of `isConsumed` **outside** the mutex as a fast-path:

```kotlin
class MutexConsumable(initiallyConsumed: Boolean = false) : Consumable {
    private var isConsumed = initiallyConsumed   // plain var
    private val mutex = Mutex()

    override suspend fun <T> tryConsume(block: suspend (Boolean) -> T) {
        if (isConsumed) {                        // RACE: read with no synchronization
            block.invoke(false)
            return
        }
        return mutex.withLock {
            if (isConsumed) {
                block.invoke(false)
            } else {
                isConsumed = true
                block.invoke(true)
            }
        }
    }
}
```

`isConsumed` is a plain `var Boolean`. The write inside the mutex is only ordered with respect to other mutex-locked sections — **plain reads outside the mutex have no happens-before relationship with the write**. Without `@Volatile` (JVM) / `AtomicReference` / `kotlinx.atomicfu`, this is a data race.

## Repro
- On Kotlin/Native (iOS/macOS), where the new memory model guarantees nothing about plain `var` cross-thread visibility, two concurrent callers may both pass the fast-path check and enter the mutex one after another. The first one observes `isConsumed=false` and consumes; the second eventually observes the cached `false`. Eventually the second one re-enters the mutex (correctly serialized) and observes `true`. Outcome converges, but during the window, both saw `false` and may have caused the caller to e.g. broadcast that the consumable was *not* yet consumed.
- On JVM, the worst case is staleness: caller B reads stale `false` after caller A's write, takes the slow path, then observes the correct value inside the mutex. Functional correctness is preserved on JVM, but the fast-path skip is statistically unreliable.

The deeper issue: the **fast-path's purpose is to avoid the mutex once consumed.** If the field is stale, we still take the mutex. So the perf optimisation regresses to no-op on Native — but no functional bug. **However**, the `tryConsume(block)` Flow extension (lines 52–59) sends `false` into a `callbackFlow` and only after `awaitClose()` does the mutex finally release. With concurrent readers contending the lock here, behaviour is correct but observability is broken.

## Root Cause
Use of plain `var Boolean` shared across coroutines without atomic access primitives. The double-checked-locking idiom requires `@Volatile` / `AtomicBoolean` to be safe.

## Impact
- Functional correctness: probably OK because the inner-mutex check is the source of truth.
- Performance / Native correctness: the optimisation is unreliable; on Native, the fast-path may actively race-violate happens-before, and depending on memory model assumptions the compiler can theoretically reorder the read.
- Most importantly, future maintainers will reason about this as "thread-safe", and add code that depends on the fast-path being correct → silent regression.

## Suggested Fix
Use `kotlinx.atomicfu.AtomicBoolean` (consistent with multiplatform):
```kotlin
private val _isConsumed = atomic(initiallyConsumed)

override suspend fun <T> tryConsume(block: suspend (Boolean) -> T) {
    if (_isConsumed.value) { block.invoke(false); return }
    mutex.withLock {
        if (_isConsumed.compareAndSet(expect = false, update = true)) {
            block.invoke(true)
        } else {
            block.invoke(false)
        }
    }
}
```
The mutex now only serialises the suspending `block`; the flag transition itself is atomic.
