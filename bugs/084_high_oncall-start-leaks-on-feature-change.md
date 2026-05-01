# `OnCallSingletonApiImpl.start()` leaks "started" on-call state across feature swaps

## Type
broken-feature

**Severity:** high

**Files:**
- `components/tools/oncall/impl/src/commonMain/kotlin/net/flipper/tools/oncall/impl/OnCallSingletonApiImpl.kt` (lines 40–56)

## Summary

`start()` calls `onCallFeatureApiFlow.collectLatest { status -> if (status != null) status.featureApi.start() }`.
When the upstream feature swaps to a *new* `FOnCallFeatureApi` instance (transport switch, reconnection,
device swap), `collectLatest` cancels the inner block but never invokes `previousFeatureApi.stop()`. The
previous feature is left in a "started" state on the device side, while the new feature is also `start()`-ed,
so two concurrent on-call sessions exist on the same device for the duration of the lifetime.

`stop()` is symmetric-broken: it calls `singleJobScope.cancelPrevious()` (returns a `Job` that is *not*
joined) and then synchronously reads `onCallFeatureApiFlow.value?.featureApi?.stop()`. A new `start()`
arriving between these two lines (or before the cancel actually completes) races against the stop and can
leave the on-call session in either state.

## Repro

1. Call `start()`.
2. Trigger a connection churn that causes `FOnCallFeatureApi` to be re-published as a new instance.
3. Observe that the *previous* feature was never `stop()`-ped — the device sees overlapping on-call
   subscriptions.
4. Now call `stop()` immediately followed by `start()`. Because `cancelPrevious()` is fire-and-forget, the
   new `start()` may launch into a `singleJobScope` whose previous job is still cancelling, racing the inner
   `featureApi.start()` call against any in-flight cleanup.

## Root cause

```kotlin
override fun start() {
    singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
        onCallFeatureApiFlow.collectLatest { status ->
            if (status != null) {
                status.featureApi.start()       // no matching .stop() on cancel of inner block
            }
        }
    }
}

override fun stop() {
    singleJobScope.cancelPrevious()             // .join() not awaited
    onCallFeatureApiFlow.value?.featureApi?.stop()
}
```

## Impact

- Resource / channel leak on device. On-call sessions may stay open after the user "ends" them.
- For long-running app sessions with multiple reconnects, the device accumulates dangling on-call streams.
- `stop()` has a TOCTOU race against `start()` and is not safe to call from multiple threads.

## Suggested fix

```kotlin
onCallFeatureApiFlow.collectLatest { status ->
    if (status == null) return@collectLatest
    try {
        status.featureApi.start()
        awaitCancellation()
    } finally {
        status.featureApi.stop()
    }
}
```

…and make `stop()` actually `suspend` and `singleJobScope.cancelPrevious().join()` so the post-stop `featureApi.stop()` is observed by the next caller.
