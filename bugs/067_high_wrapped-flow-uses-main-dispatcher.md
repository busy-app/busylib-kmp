# `WrappedFlow.watch` always launches its callback scope on `Dispatchers.Main`

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/wrapper/src/commonMain/kotlin/net/flipper/busylib/core/wrapper/CFlow.kt` lines 16–38

## Summary
`WrappedFlow.watch` (and its sibling `WrappedStateFlow.watch`, `WrappedSharedFlow.watch`) ultimately invoke the private `Flow<T>.onEach(...)` extension, which hard-codes:

```kotlin
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```

This makes three implicit assumptions that are not always satisfied for the Swift / Apple / desktop consumers of `BusyLibKMP`:

1. `Dispatchers.Main` is initialised. On Apple multiplatform, the Main dispatcher exists; on JVM/desktop it is **only** initialised after `kotlinx-coroutines-swing` / `kotlinx-coroutines-javafx` is loaded. A pure JVM consumer that depends on `wrapper` will throw `IllegalStateException("Module with the Main dispatcher had failed to initialize.")` at the first call to `watch`.
2. The consumer wants callbacks on Main. For Swift, this is *usually* what you want, but it locks consumers out of running flows on background queues even when they explicitly need to.
3. The callbacks may never run if the Main dispatcher is not actually pumped by the consumer (pure-headless tests, server-side JVM consumers).

```kotlin
private fun <T> Flow<T>.onEach(...): Closeable {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    onEach(onEach).catch { onError(it) }.onCompletion { onComplete(); scope.cancel() }.launchIn(scope)
    return object : Closeable { override fun close() { scope.cancel() } }
}
```

## Repro
```kotlin
// Pure JVM consumer, no JavaFX/Swing classpath
val flow = MutableStateFlow(0).wrap()
flow.watch(onEach = { println(it) })
// java.lang.IllegalStateException: Module with the Main dispatcher had failed to initialize.
// For tests, set Dispatchers.setMain(...).
```

## Root Cause
- Hard-coded `Dispatchers.Main` instead of accepting a dispatcher parameter or using `Dispatchers.Default` as a fallback.
- The internal `onEach` is `private`, making it impossible for consumers to override.

## Impact
- `WrappedFlow.watch` is the **only** way Swift consumes flows from this SDK (since SKIE FlowInterop is intentionally disabled per AGENTS.md). Crashes here propagate directly to consumer apps.
- Pure-JVM consumers (sample app's tests, future server integrations) cannot use `watch` without manually injecting Main.

## Suggested Fix
Accept a dispatcher (default Main) and tolerate its absence:

```kotlin
private fun <T> Flow<T>.onEachInternal(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    ...
): Closeable {
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    ...
}

class WrappedFlow<T : Any?>(private val origin: Flow<T>) : Flow<T> by origin {
    fun watch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        onEach: (T) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Closeable = origin.onEachInternal(dispatcher, onEach, onComplete, onError)
}
```
For Swift backwards-compat, retain the no-arg `watch` overload but defer to `Dispatchers.Main.immediate`.
