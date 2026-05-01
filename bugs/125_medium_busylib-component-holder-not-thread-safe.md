# `BusyLibComponentHolder.components` is a non-synchronised `mutableSetOf` mutated from any thread

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/di/src/commonMain/kotlin/net/flipper/busylib/core/di/BusyLibComponentHolder.kt`

## Summary
```kotlin
object BusyLibComponentHolder {
    val components = mutableSetOf<Any>()

    inline fun <reified T> component(): T = components
        .filterIsInstance<T>()
        .single()
}
```

`components` is a `mutableSetOf<Any>()` (HashSet, non-synchronised). The DI graph constructs and registers components from any thread (initial setup via Swift bridge runs on Main, but Anvil generated code may register from a different thread depending on KSP output). Concurrent mutation of `mutableSetOf` is unsafe — `HashSet.add` racing with `iterator()` produces `ConcurrentModificationException` on JVM, undefined behaviour on Native.

`component()` performs `filterIsInstance(...).single()`, iterating the set without any external lock. If a registration is in flight, the iteration can throw or produce wrong results.

## Repro
```kotlin
// Thread A
BusyLibComponentHolder.components.add(component1)
// Thread B (concurrent)
BusyLibComponentHolder.component<MyApi>()   // ConcurrentModificationException possible
```

## Root Cause
- `mutableSetOf` is not thread-safe.
- The `inline fun` defers `component()`'s iteration to call site, but the iteration is still over the unsynchronised set.

## Impact
- Crash on app startup / device-graph initialisation if the DI registration happens on multiple threads.
- Sporadic test failures with parallel test runners.

## Suggested Fix
Use a synchronised collection or AtomicReference:
```kotlin
object BusyLibComponentHolder {
    private val _components = atomic(persistentSetOf<Any>())
    fun add(component: Any) { _components.update { it.add(component) } }
    val components: Set<Any> get() = _components.value
    inline fun <reified T> component(): T = components.filterIsInstance<T>().single()
}
```
Or wrap mutations in a `synchronized(this) { … }` block.
