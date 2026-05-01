# Watchers do not auto-restart on flow termination — a single uncaught exception silently disables them

## Type
lack-of-feature

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/changename/src/commonMain/kotlin/net/flipper/bsb/watchers/changename/BUSYLibNameWatcher.kt` lines 38-66
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/desktop/src/commonMain/kotlin/net/flipper/bsb/watchers/desktop/DesktopLanBarsWatcher.kt` lines 25-35
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/CloudFetcherWatcher.kt` lines 36-50
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/CloudProvisioningWatcher.kt` lines 47-75
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/HardwareIdProvisioningWatcher.kt` lines 40-69

## Summary

Each watcher's `onLaunch` body is a single `singleJobScope.launch { /* one collect */ }`.
The pattern across watchers is:

```kotlin
override fun onLaunch() {
    singleJobScope.launch(SingleJobMode.X) {
        upstream.flatMapLatest { ... }.collect { ... }   // or collectLatest
    }
}
```

`onLaunch` is a one-shot startup hook (`InternalBUSYLibStartupListener`).
Nothing in the library re-invokes it on watcher failure. If the inner
flow:

- terminates because of an upstream completion (e.g. a `flowOf()` with no
  values that gets `flatMapLatest`'d to an empty inner stream — this is
  benign because the outer flow does not complete, but if any operator
  introduces a `take(1)` upstream the watcher would silently disappear),
- throws (and the watcher does not wrap with `runSuspendCatching`, see
  `medium_cloud-fetcher-watcher-no-error-isolation.md`),
- is cancelled by a parent (e.g. the supervisor scope dies due to an
  unrelated child),

then the watcher is permanently disabled until process restart.

## Repro

For `HardwareIdProvisioningWatcher`:

1. `featureProvider.get<FRpcFeatureApi>()` happens to throw on a corrupt
   feature emission (e.g. a feature constructor IllegalStateException).
2. The throw propagates through `flatMapLatest` → `collectLatest` → the
   `launch` block. The job ends in failure.
3. `MutexSingleJobCoroutineScope` does not restart it.
4. Subsequent reconnects with valid features no longer trigger
   hardware-id provisioning.

## Root Cause

There is no supervisor watchdog that restarts a failed watcher. The
`SingleJobCoroutineScope` does not have a "respawn on unexpected
completion" mode. AGENTS.md mentions `exponentialRetry` but it is not used
at the watcher level.

## Impact

- Latent silent disablement of background sync.
- No telemetry to surface the dead watcher; only dispersed `error { ... }`
  logs.

## Suggested Fix

1. Wrap each watcher's outer collect in
   ```kotlin
   while (currentCoroutineContext().isActive) {
       runSuspendCatching { upstream.collect { ... } }
           .onFailure { error(it) { "watcher crashed, restarting" } }
   }
   ```
   or rely on a top-level `BusyLibSupervisor` that restarts dead listeners.
2. Add tests that throw mid-flow and assert the watcher recovers on the
   next emission.
