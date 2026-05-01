# `PreviousVersionFlowProvider` can hang or miss `UpdateFinished`/`UpdateFailed` events on fast updates

## Type
broken-feature

**Severity:** medium

**Files:**
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/updater/api/PreviousVersionFlowProvider.kt` (lines 42–73)

## Summary

`getPreviousVersionFlow(...)` builds a `channelFlow` that, per BUSY Bar, sequentially:

1. Awaits the *first* non-null version → emits `(previous=null, current=before)`.
2. `fwUpdateFlow.filterIsInstance<FwUpdateState.Updating>().first()` — wait for an Updating state.
3. `fwUpdateFlow.filter { it !is Updating }.first()` — wait for it to leave Updating.
4. Awaits new version → emits `(previous=before, current=after)`.

There are several hazards:

- **Step 2 can race.** `fwUpdateFlow` is the `state` flow obtained via `stateIn(scope, SharingStarted.Lazily,
  FwUpdateState.Pending)`. By the time the channelFlow starts collecting, the firmware install may already
  have moved past `Updating` (especially on LAN updates that flip Uploading→Updating→Pending in milliseconds
  while the device reboots). `filterIsInstance<Updating>().first()` will then **hang forever** waiting for an
  `Updating` it has already missed.
- **Step 3 immediate-emit hazard.** Conversely, if `fwUpdateFlow.value` is currently `Updating`, step 2 sees
  it instantly, then step 3 immediately sees the next state — which can be `Pending` if the orchestrator
  reset before the install actually finished. The provider then declares the update finished and emits
  `UpdateFinished` with a `currentVersion == previousVersion`, which the consumer interprets as `UpdateFailed`
  even though the device may still be installing.
- **Step 4 unbounded wait.** Awaiting the new version after the device reboots blocks emission of further
  events until the device reconnects. If the device is bricked / never reconnects, the loop never enters its
  next iteration.

The outer `while (currentCoroutineContext().isActive)` only restarts the inner block after `getCurrentDevice
Flow().first()` completes — but the inner `mapLatest { ... }.first()` blocks in step 4 forever, so the
"Reset versionTransition on BusyBar change" comment never gets a chance to take effect when the user switches
devices mid-update.

## Repro

1. Trigger a fast LAN update. Subscribe to `events`. Observe that the same install can both miss
   `UpdateFinished` (if `Updating` was missed) and emit `UpdateFailed` falsely (if the cycle ran too fast).

## Root cause

`StateFlow.first()` returns the current value immediately, but the provider treats it as a transition signal.
There is no replay tracking of "have I seen this transition yet."

## Impact

- Users sometimes see "Update failed" UI after a successful update.
- Users sometimes never see "Update completed" after a successful update.
- A bricked / never-returning device leaves the state machine permanently stuck.

## Suggested fix

- Track previous and current `FwUpdateState` explicitly:
  ```kotlin
  fwUpdateFlow.zipWithNext().filter { (prev, next) -> prev is FwUpdateState.Updating && next !is FwUpdateState.Updating }.first()
  ```
- Add a timeout to step 4 so the loop can re-enter.
- Use `BusyBar` `unique_id` change as a hard reset signal that interrupts step 4 (currently nothing does).
