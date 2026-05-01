# Cloud transport silently destroyed on transient `linked=false`

## Type
broken-feature

**Severity:** critical

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/CloudProvisioningWatcher.kt` lines 67-87, 98-149

## Summary

`CloudProvisioningWatcher` reacts to every emission of
`FRpcCriticalFeatureApi.currentAccountInfo` (a `StateFlow<RpcLinkedAccountInfo?>`).
When that emission carries `cloudId == null` and the local device has a cloud
transport, the watcher unconditionally strips the cloud transport from storage,
and if the device has no other transports (the common BUSY-Bar-on-LAN case
where `cloud` is the sole transport from `CloudFetcherWatcher`), the entire
device record is deleted from local storage.

There is no debouncing, no consensus across multiple emissions, and no
verification against the user's cloud account — a single transient
`RpcLinkedAccountInfo(linked=false, cloudId=null)` permanently destroys data.

```kotlin
if (cloudId == null) { // Local and cloud, not connected to cloud - remove cloud connection
    modifyOrDelete(original) {
        it.copy(cloud = null)        // returns null when no other transport
    }
    return
}
```

`modifyOrDelete` calls `removeDevice(original.uniqueId)` when the copy returns
null. That deletion is committed in the same transaction.

## Repro

1. Cloud-only device exists in storage (added by `CloudFetcherWatcher` after the
   user signs in on a different host — there is no LAN/BLE transport stored).
2. User opens the app while connected to the device. The device boots its RPC
   stack and on the very first `/api/account/info` reply transiently reports
   `linked=false, cloudId=null` (firmware pre-provisioning, network glitch
   between device and BUSY backend, NTP-driven account-info refresh, etc.).
3. Watcher fires `updateBUSYBar(cloudId = null, original = cloudOnlyDevice)`.
4. `original.copy(cloud = null)` returns `null` (no other transport).
5. `modifyOrDelete` calls `removeDevice` → device record gone.
6. The next `currentAccountInfo` emission with the correct linked user no
   longer has a matching local device, so the cloud transport is never
   restored automatically.

## Root Cause

The watcher treats a single nullable RPC value as authoritative ground truth.
There is:
- no retry / re-fetch via `FRpcCriticalFeatureApi.invalidateLinkedUser` before
  acting,
- no cross-check against `BusyCloudBarsApi.getBarsList` to confirm the cloud
  no longer knows the device,
- no minimum-stability window (e.g. require N consistent emissions or
  `debounce`),
- no distinction between "device firmware says I am unlinked" (could be wrong
  during boot) and "BUSY cloud backend confirms unlink" (definitive).

The deletion path in `modifyOrDelete` is also too eager — for cloud-only
devices, "the device says it is unlinked" and "the device record should be
purged" are conflated.

## Impact

- Permanent loss of the user's only handle to a paid/provisioned device.
  The user has to re-pair from scratch (cloud account → device link code →
  device-side acknowledgement) to get the device back into local storage.
- Silently breaks `FConnectionService.getExpectedState()` — once the device is
  removed, `currentDevice` becomes null, the connection loop disconnects, and
  the user sees an empty device list in the UI.
- Worsens during flaky networks because every `invalidateLinkedUser` retry
  that returns `linked=false` triggers another deletion attempt.

## Suggested Fix

1. Stop deleting the device when stripping cloud. Either:
   - leave the device record intact with `cloud = null` and let
     `DesktopAutoPurger` / explicit user action remove it, **or**
   - require corroboration from `CloudFetcher` (the device must also be
     missing from the user's cloud bars list) before deletion.
2. Add a debounce (1-2 s) and/or require the same `cloudId` value across at
   least two consecutive emissions before reacting, mirroring the workaround
   added in `AutoProvisioningTimeZone (#270)`.
3. If `currentAccountInfo.value` is the StateFlow's initial sentinel emitted
   on subscribe, drop it explicitly with `.drop(1)` or filter on a known
   "freshly populated" marker rather than relying on `linkedInfo == null`.
