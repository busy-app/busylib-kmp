# `launchOnCompletion` runs the cleanup block only on cancellation, not on normal scope completion

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/CoroutineScopeExt.kt` lines 11–25

## Summary
The implementation registers `awaitCancellation()` and runs the cleanup in `finally`:

```kotlin
fun CoroutineScope.launchOnCompletion(block: suspend () -> Unit) {
    launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                try { block.invoke() } catch (t: Throwable) { error(t) {…} }
            }
        }
    }
}
```

`awaitCancellation()` returns `Nothing` and only completes via `CancellationException`. If the surrounding `CoroutineScope` finishes **normally** (e.g. it was a `coroutineScope { ... }` block whose body returned, or a structured-concurrency `Job` that was completed via `.complete()` rather than cancelled), the launched coroutine is also cancelled by structured concurrency *before* its body has a chance to run, so the `try/finally` does still execute on cancellation in that path.

But: if the parent `Job` completes "exceptionally" via a non-CE throwable (e.g. a child throws), parent goes from completing → cancelling. Children get CE, OK. So in practice cleanup runs for all completion modes that propagate through the parent.

The real issue is more subtle: in the API name `launchOnCompletion` users naturally expect *guaranteed* execution. With `withContext(NonCancellable)` inside `finally`, cleanup runs — but if `block.invoke()` itself suspends and the CoroutineScope is being torn down (e.g. via `cancel()` on a SupervisorJob with no associated dispatcher), `withContext(NonCancellable)` will run on the existing dispatcher; if that dispatcher has already shut down (e.g. `Executors.newWorkStealingPool().asCoroutineDispatcher()` whose underlying `ExecutorService` was closed elsewhere), the cleanup will hang or throw `RejectedExecutionException` swallowed by the outer `try/catch`.

Additionally, `DefaultObjectCache.init { scope.launchOnCompletion { clear() } }` (file `DefaultObjectCache.kt:113`) depends on `launchOnCompletion` running when the scope completes. Because `DefaultObjectCache.clear()` itself takes the mutex, if the dispatcher is dead at that point the cleanup may silently drop.

## Repro
1. Create a scope on a dedicated single-thread dispatcher.
2. `launchOnCompletion { criticalCleanup() }` on that scope.
3. Cancel the scope and immediately close the underlying executor.
4. Observe `criticalCleanup` either hangs or its log message is the swallowed `RejectedExecutionException`.

## Root Cause
- `awaitCancellation()` semantics tie the cleanup to coroutine cancellation, not to the broader notion of "scope completion" (which can be normal or cancellation).
- `withContext(NonCancellable)` does not switch dispatchers — it only ignores cancellation. The dispatcher continues to be the original; if that dispatcher is dead, the cleanup cannot proceed.
- The `try/catch` swallows unexpected exceptions silently.

## Impact
- Cache / resource cleanup failures are silently logged (not propagated), so callers cannot react.
- For platform-specific dispatchers (`newWorkStealingPool().asCoroutineDispatcher()` on Android/JVM in `FlipperDispatchers`), the cleanup may not run after dispatcher shutdown.

## Suggested Fix
1. Use `Job.invokeOnCompletion` directly — it fires on **any** completion (success or cancellation) and is dispatcher-independent (it runs on the canceller's thread). For suspending cleanup, use `Job.invokeOnCompletion { GlobalScope.launch(NonCancellable) { … } }` cautiously, or expose a dedicated cleanup dispatcher.
2. At minimum, document the dispatcher-survival requirement in the KDoc of `launchOnCompletion` so callers know not to compose it with disposable executors.
