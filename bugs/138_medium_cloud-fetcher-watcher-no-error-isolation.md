# `CloudFetcherWatcher.onLaunch` does not wrap transaction in `runSuspendCatching`

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/CloudFetcherWatcher.kt` lines 36-50

## Summary

```kotlin
override fun onLaunch() {
    singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
        cloudFetcher.getBarsFlow().collectLatest { cloudBars ->
            persistedStorage.transactionInternal {
                ...
                invalidateCloudBars(cloudBars)
            }
        }
    }
}
```

The body of `transactionInternal` mutates storage. If any helper inside
`invalidateCloudBars` (`mergeExisting`, `addNew`, `removeUnlinked`) throws —
e.g. `Uuid.parseOrNull` returning unexpected values, `addOrReplace` failing,
a malformed cloud bar — the exception propagates out of `collectLatest`,
which terminates the entire flow. Because the outer `singleJobScope.launch`
uses `CANCEL_PREVIOUS` and there is no automatic restart loop, **the watcher
permanently stops syncing** for the lifetime of the process until something
else triggers `onLaunch` again (which the listener never does — it is a
single-shot startup hook).

The peer watcher `CloudProvisioningWatcher` correctly wraps its
`onNewLinkedInfo` call in `runSuspendCatching` for exactly this reason
(lines 67-73). `CloudFetcherWatcher` did not get the same treatment.

## Repro

1. A WS event delivers a payload that the model layer cannot fully process —
   e.g. a `BUSYBarWebSocket.cloudId` that conflicts with a stored UUID, or
   a hook (`AlwaysActiveHook`, `RemoveDuplicateCloudHook`) throws on a
   degenerate state inherited from another watcher.
2. The transaction throws inside `collectLatest`.
3. The exception is rethrown up through `collectLatest` → `getBarsFlow` →
   the watcher's `launch` block. The job completes exceptionally.
4. `MutexSingleJobCoroutineScope` records the (cancelled) job. No restart.
5. Subsequent cloud changes (link/unlink/rename on the user's web UI) are
   never reflected in local storage until the app is fully restarted.

## Root Cause

Missing exception isolation around the storage transaction. AGENTS.md
explicitly mandates `runSuspendCatching` instead of `runCatching` inside
suspend code; the hand-rolled approach here uses neither.

## Impact

- Single transient exception during cloud sync silently disables cloud sync
  for the rest of the session.
- Hard to detect because nothing is logged at the supervisor level when a
  watcher dies.

## Suggested Fix

1. Wrap the transaction in `runSuspendCatching` and log/onFailure, mirroring
   `CloudProvisioningWatcher`:
   ```kotlin
   .collectLatest { cloudBars ->
       runSuspendCatching {
           persistedStorage.transactionInternal { ... }
       }.onFailure { error(it) { "Failed to sync cloud bars" } }
   }
   ```
2. Optionally wrap the entire `singleJobScope.launch` body in a retry-with-
   logging loop so a flow-terminating throw cannot disable the watcher
   permanently.
