# `HardwareIdProvisioningWatcher` re-fetches hardware ID on every reconnect, but never on user-initiated state refresh

## Type
broken-feature

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/HardwareIdProvisioningWatcher.kt` lines 40-69

## Summary

The watcher decides whether to fetch the device serial number using
`state.device.hardwareId` from the orchestrator's connect status, not from
the persisted storage. The orchestrator's `state.device` is whatever
`BUSYBar` was passed into `connectIfNot(config)` — it is a snapshot, not a
live view of storage.

```kotlin
orchestrator.getState().flatMapLatest { state ->
    if (state is FDeviceConnectStatus.Connected && state.device.hardwareId == null) {
        featureProvider.get<FRpcFeatureApi>().map { it to state.device }
    } else {
        flowOf()
    }
}
```

Two distinct bugs come from this:

1. **Stale hardwareId snapshot — re-fetch on every reconnect.** When the
   watcher writes the freshly fetched `serialNumber` into storage,
   `state.device` (held in the orchestrator's `FDeviceConnectStatus.Connected`)
   keeps the old `hardwareId = null`. On the next reconnect, the orchestrator
   still emits `Connected` with the same stale `BUSYBar`, the predicate fires
   again, and the RPC call to `getDeviceStatus()` is repeated.
2. **Wrong device gets the hardwareId on rapid switches.**
   `featureProvider.get<FRpcFeatureApi>()` emits whenever the feature status
   changes, while `state.device` is captured once per outer `flatMapLatest`
   iteration. If the device transitions Connected(A) → Disconnected →
   Connected(B), but the inner `featureProvider.get` flow happens to deliver
   `Supported` for B *before* the outer flow re-evaluates, the inner pair
   `(rpcApiStatus, device)` may carry mismatched components — `device = A`
   from the captured outer scope, but `rpcApiStatus.featureApi` belongs to B.
   Calling `getDeviceStatus()` on B's RPC and writing the result to A's
   `uniqueId` mis-associates the hardware id.

The second issue is mitigated by the `collectLatest` cancelling on outer
state changes, but the cancellation is racy with the inner suspend
`getDeviceStatus()` — the serial is fetched, then `onSuccess` writes to
storage *after* cancellation has been requested but before the next
`ensureActive` check.

## Repro (Issue 1, redundant fetches)

1. Device connects. Storage has no hardwareId. Orchestrator state holds
   `BUSYBar(hardwareId = null)`.
2. Watcher fetches serial, writes to storage. Storage now has hardwareId.
3. Device disconnects.
4. Device reconnects. Orchestrator state still uses
   `BUSYBar(hardwareId = null)` because nothing in the connect path re-reads
   storage to refresh that field — `connectIfNot(config)` is called with
   whatever the loop computed at the time.
5. Watcher fires another `getDeviceStatus()` RPC even though the answer is
   already known. Wasteful at minimum; on flaky links it can also overwrite
   a known-good hardwareId with a transient failure when combined with
   `runSuspendCatching`-style error handling.

## Repro (Issue 2, mismatch)

1. Device A connects, predicate emits `(SupportedA, deviceA)`.
2. Before `getDeviceStatus()` returns, A disconnects, B connects.
3. `flatMapLatest` cancels the prior inner stream and issues a new one for B.
   `collectLatest` cancels the old `getDeviceStatus()` if it has not yet
   resumed. **However** if the suspension point is past the body of
   `getDeviceStatus()` and at `.onSuccess { ... }`, the success block runs
   before cancellation propagates and writes the result to storage with
   `device.uniqueId == "A"`.

## Root Cause

- `state.device.hardwareId == null` is a stale check. The watcher should
  re-read storage at fetch time, e.g.
  `persistedStorage.transactionInternal { getDevice(state.device.uniqueId)?.hardwareId == null }`.
- `getDeviceStatus()` is not framed in `runSuspendCatching` and the
  side-effecting `onSuccess` block is not gated on `coroutineContext.isActive`.

## Impact

- Unnecessary RPC traffic on reconnect.
- On rapid A→B device switches, B's serial may be written to A's storage
  record (or vice versa), corrupting `hardwareId` ↔ `uniqueId` association
  used by `CloudFetcherWatcher.mergeExisting` (it joins on `hardwareId`!).
  This in turn leads to incorrect cloud-bar merging.

## Suggested Fix

1. Replace the `state.device.hardwareId == null` predicate with a check that
   reads the latest storage value:
   ```kotlin
   persistedStorage.getAllDevicesFlow().asFlow()
       .map { devices -> devices.firstOrNull { it.uniqueId == state.device.uniqueId } }
       .map { it?.hardwareId == null }
   ```
2. In the success path, verify `getDevice(device.uniqueId) != null` (still
   present in storage) before writing — `addOrReplace` should not be called
   on a forgotten device.
3. Wrap `getDeviceStatus()` in `runSuspendCatching` and `ensureActive()`
   before writing.
