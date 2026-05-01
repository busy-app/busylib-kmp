# `CloudProvisioningWatcher.onNewLinkedInfo` writes cloud info to a device that may no longer be the connected one

## Type
broken-feature

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/CloudProvisioningWatcher.kt` lines 60-87, 131-149

## Summary

The inner mapping captures the connected device's `uniqueId` at flow-build
time:

```kotlin
is FFeatureStatus.Supported<FRpcCriticalFeatureApi> -> {
    rpcApiStatus.featureApi.currentAccountInfo.map { it to state.device.uniqueId }
}
```

The downstream `collectLatest` runs `onNewLinkedInfo(linkedInfo, deviceId)`
which calls `persistedStorage.transactionInternal { getDevice(deviceId)?.let { updateBUSYBar(...) } }`.

Race window: between the `currentAccountInfo` emission being captured and
the transaction acquiring storage's mutex, a different coroutine
(`CloudFetcherWatcher` or user-driven `forgetDevice`) can `removeDevice(deviceId)`.
The `getDevice(deviceId)` call inside the transaction returns null, the
`let` body is skipped — that path is safe.

But the *creation path* (line 142-148) creates a brand-new device with a
fresh `Uuid.random().toString()` and switches current device to it, copying
*transports* from `original` regardless of whether `original` still exists
in storage:

```kotlin
val newBUSYBar = original.copyTransports(uniqueId = Uuid.random().toString())
    .addTransport(cloud = BUSYBar.ConnectionWay.Cloud(cloudId))
addOrReplace(newBUSYBar)
setCurrentDevice(newBUSYBar)
```

`original` is the value returned by `getDevice(deviceId)` at the start of
the transaction — that part is consistent. **However** the `original` was
the value of the device when its `currentAccountInfo` was emitted, and the
intent of "create a new device because cloud says it's linked elsewhere"
assumes that the connected device is still that same `original`. If the
user has just `forgetDevice`d the original and the orchestrator then
reconnected to a different device, the watcher will create a "new" device
with the BLE/LAN transports of the *forgotten* device — re-resurrecting
data the user explicitly asked to forget.

## Repro

1. `device-1` is connected via BLE. `currentAccountInfo` emits
   `RpcLinkedAccountInfo(linked=true, cloudId=NEW)` where NEW differs from
   any local cloud id.
2. The watcher's `collectLatest` lambda starts — it calls
   `runSuspendCatching { onNewLinkedInfo(NEW, "device-1") }`.
3. Before `transactionInternal` acquires the storage mutex, the user
   triggers `FConnectionService.forgetDevice(device-1)` which removes the
   device.
4. `transactionInternal` acquires the mutex, `getDevice("device-1")` returns
   null — the `?.let` branch is skipped. **Safe in this case.**

But invert the timing:
1. `currentAccountInfo` for the original instance of `device-1` emits and
   the watcher captures `(linkedInfo, "device-1")`.
2. The transaction starts before forget. `original` is read.
3. `linkedInfo.cloudId` differs from `original.cloud?.deviceId`. The
   "create new device" branch executes, doing `Uuid.random()` and
   `addOrReplace(newBUSYBar)`.
4. Concurrently the user-issued `forgetDevice(device-1)` was queued behind
   the same mutex; once this transaction releases, the forget removes
   device-1 — but the just-resurrected `newBUSYBar` (with cloned BLE/LAN
   transports of `device-1`) survives.

The resulting state: a phantom device record with BLE MAC / LAN IP that the
user thought they just forgot.

## Root Cause

The "create-and-switch" branch fires unconditionally on
`cloudConnection != null && cloudId != null && cloudConnection.deviceId != cloudId`.
There is no policy gate (e.g. confirm with the user, or wait for the cloud
list watcher to confirm the new cloud id corresponds to a tracked device).

Combined with `copyTransports`, which copies BLE/LAN secrets, the watcher
performs a privilege-bearing operation (reusing pairing data) silently.

## Impact

- A forgotten device can re-appear under a fresh `uniqueId` with the same
  BLE/LAN transport data, partially defeating "forget" semantics.
- The newly created device becomes the current device automatically, so the
  next reconnect cycle will attempt to connect to it.

## Suggested Fix

1. Re-read `getCurrentDevice()` inside the transaction and only run the
   `copyTransports` path if `original.uniqueId == getCurrentDevice()?.uniqueId`.
2. Drop `setCurrentDevice(newBUSYBar)` and let `DesktopActiveDevice` (or a
   user gesture) decide which device is current.
3. Rather than cloning transports, create a cloud-only device record and
   leave the original BLE/LAN transports tied to the original device.
