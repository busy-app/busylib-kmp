# `DesktopLanBarsWatcher.onLaunch` uses `SKIP_IF_RUNNING` and adds hooks idempotently — but the activating transaction may run before hook addition is observed

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/desktop/src/commonMain/kotlin/net/flipper/bsb/watchers/desktop/DesktopLanBarsWatcher.kt` lines 25-35
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/config/impl/src/commonMain/kotlin/net/flipper/bridge/connection/config/impl/FDevicePersistedStorageImpl.kt` lines 43-47

## Summary

```kotlin
override fun onLaunch() {
    singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
        persistedStorage.addHook(
            DesktopEmptyFiller(),
            DesktopAlwaysLan(),
            DesktopActiveDevice(),
            DesktopAutoPurger()
        )
        persistedStorage.transaction { } // Activate all hooks
    }
}
```

Two correctness concerns:

1. **No coordination with other startup listeners.** All
   `InternalBUSYLibStartupListener::onLaunch` implementations are invoked
   in parallel from the same DI graph. `CloudFetcherWatcher`,
   `CloudProvisioningWatcher`, and `BUSYLibNameWatcher` may run their first
   `transactionInternal` *before* `DesktopLanBarsWatcher` has finished
   `addHook(...)`. Their transactions will commit without the desktop hooks
   firing — meaning early storage mutations bypass `DesktopAlwaysLan` (no
   LAN transport added), `DesktopAutoPurger` (no LAN-only purge), etc. The
   no-op `transaction { }` only guarantees the hooks are run *for that
   particular empty transaction*, not for transactions that were already
   serialized through the storage mutex before the hooks were added.

2. **`addHook` and `transaction` are independent suspensions.** The
   `addHook` call grabs `mutex` in `add_hook` mode, releases it; another
   coroutine can interleave a transaction between `addHook` returning and
   `transaction { }` starting. That competing transaction will run with
   the *new* hook list (good), but if it preempts `DesktopLanBarsWatcher`'s
   own activating no-op transaction, the no-op transaction becomes
   redundant — a minor logical inconsistency that argues the
   `transaction { }` activation is performative, not load-bearing.

## Repro (Issue 1)

1. App boots. DI graph constructs all `InternalBUSYLibStartupListener`s.
2. The graph's `onLaunch` dispatcher calls `BUSYLibNameWatcher.onLaunch()`,
   `CloudFetcherWatcher.onLaunch()`, `DesktopLanBarsWatcher.onLaunch()` in
   some order on the default dispatcher. They are scheduled in parallel.
3. `BUSYLibNameWatcher` reaches its first storage write before
   `DesktopLanBarsWatcher` reaches `addHook`.
4. The write commits. The desktop hooks (`AlwaysLan`, `EmptyFiller`,
   `AutoPurger`, `ActiveDevice`) do not run.
5. Storage now contains a device that should have had LAN added by
   `DesktopAlwaysLan` — but doesn't.

## Root Cause

The library has no defined startup ordering for listeners. The mitigation
in `DesktopLanBarsWatcher` (the activating no-op transaction) is local: it
re-runs hooks once, but cannot retroactively rewrite earlier transactions
that were committed without the hooks installed.

## Impact

- On macOS / desktop platforms, the first device added at app boot can
  miss the desktop policy hooks, leaving (e.g.) a cloud-only device with no
  LAN transport even though the desktop platform always wants LAN as a
  fallback.
- Symptom is sporadic and platform-specific — easy to misdiagnose.

## Suggested Fix

1. Register desktop hooks during DI graph construction (synchronously) rather
   than asynchronously inside `onLaunch`. The hooks have no startup-order
   dependencies — they are pure `TransactionHook` instances.
2. Or: introduce a "startup-listener-priority" so `DesktopLanBarsWatcher`'s
   `onLaunch` is awaited before other listeners.
3. Make the activating `transaction { }` an explicit `transactionInternal`
   that re-runs the hook list against the current storage state (i.e. apply
   the hooks against existing devices once, not just the empty diff).
