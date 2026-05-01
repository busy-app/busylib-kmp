# BUSYLibNameWatcher unconditionally overwrites local device name on every reconnect / rename round-trip

## Type
broken-feature

**Severity:** critical

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/changename/src/commonMain/kotlin/net/flipper/bsb/watchers/changename/BUSYLibNameWatcher.kt` lines 38-66

## Summary

`BUSYLibNameWatcher` blindly forwards every emission of `FSettingsFeatureApi.getDeviceName()` (a `WrappedStateFlow<String>`) into persisted storage as the
device's `humanReadableName`. There is no de-duplication against the
storage value and no concept of "the user just renamed locally and the device
hasn't acknowledged yet". As a result:

1. **User-rename loopback race.** When the user renames the device locally
   via UI (`persistedStorage.transaction { addOrReplace(it.copy(humanReadableName = "MyName")) }`) and *immediately* — while the device is still emitting its
   pre-rename name from the cached `StateFlow<String>` value — the watcher
   reads the stale device-side name and writes it back, undoing the user's
   change.
2. **Reconnect overwrite.** On every reconnect the `combine` re-fires, the
   `Supported` branch resubscribes to `getDeviceName()`, the StateFlow replays
   its latest value (the device's name as the device sees it), and the watcher
   commits a `transaction` that resets `humanReadableName`.

## Repro

### Scenario A — user rename lost
1. Persisted device has `humanReadableName = "Old"`.
2. Device's `getDeviceName()` StateFlow currently holds `"Old"`.
3. User taps Rename in the UI → app calls `transaction { addOrReplace(it.copy(humanReadableName = "New")) }`. Storage now has `"New"`.
4. UI also calls `FSettingsFeatureApi.setDeviceName("New")` over RPC — request
   is in-flight.
5. Watcher's collector wakes up because storage emitted (it doesn't, but the
   coroutine is concurrently scheduled anyway). The StateFlow still emits
   `"Old"` (or replays it on a re-subscription due to a transient feature
   status change). Watcher calls `updateDeviceName(deviceId, "Old")`.
6. Storage now contains `"Old"` again. The user sees their rename revert.

### Scenario B — reconnect overwrite
1. Persisted device has `humanReadableName = "MyBar"`.
2. Device firmware's stored name is `"BUSY Bar"` (default; user only renamed
   locally because the device-side rename never succeeded).
3. App reconnects. `featureProvider.get<FSettingsFeatureApi>()` emits
   `Supported`. `flatMapLatest` subscribes to `getDeviceName()`, which
   immediately replays `"BUSY Bar"`.
4. Watcher writes `"BUSY Bar"` to storage, silently destroying the user's
   local label every time the app starts.

## Root Cause

The watcher has no notion of "authoritative source." It treats the device as
always correct. There is no:
- read-modify-write check (`if (current.humanReadableName != deviceName) ...`),
- debounce / settle window,
- distinction between "device pushed a fresh name event" and "StateFlow
  replayed its cached value on resubscription",
- guard for "user just renamed in storage and the device has not yet
  acknowledged."

## Impact

- User renames are silently reverted on every reconnect.
- Rapid local rename followed by reconnect produces a flapping name that
  toggles between user input and device-reported value.
- No tests exist for this watcher (the `watchers/changename` module has no
  `commonTest` directory), so the regression is not caught by CI.

## Suggested Fix

1. Read storage inside `updateDeviceName` and skip the write when
   `current.humanReadableName == deviceName`.
2. Drop the first `getDeviceName()` emission per subscription with `.drop(1)`
   so reconnect does not re-emit an already-known value.
3. Add a reverse-direction guard: when local rename is in flight (UI-driven
   `setDeviceName` not yet acknowledged), suppress writes by tracking a
   "pending local rename" timestamp/value.
4. Add tests in `watchers/changename/src/commonTest` covering
   reconnect-no-overwrite, user-rename-not-lost, and device-driven rename
   propagation.
