# Duplicate-removal hooks can leave `currentSelectedDeviceId` pointing at a removed device

## Type
broken-feature

**Severity:** medium

**Files:**
- `components/bridge/config/impl/src/commonMain/kotlin/net/flipper/bridge/connection/config/impl/hooks/RemoveDuplicateHardwareIdHook.kt` (lines 16–48)
- `components/bridge/config/impl/src/commonMain/kotlin/net/flipper/bridge/connection/config/impl/hooks/RemoveDuplicateCloudHook.kt` (lines 16–48)
- `components/bridge/config/impl/src/commonMain/kotlin/net/flipper/bridge/connection/config/impl/PersistedStorageTransactionScopeImpl.kt` (lines 52–66)

## Summary

Both `RemoveDuplicateHardwareIdHook` and `RemoveDuplicateCloudHook` have the same shape:

```kotlin
val currentDevice = getCurrentDevice()
...
duplicates.forEach { device ->
    if (device.uniqueId != best.uniqueId) {
        removeDevice(device.uniqueId)
        if (currentDevice?.uniqueId == device.uniqueId) {
            setCurrentDevice(best)
        }
        best = mergeBBIfEmpty(best, device)
    }
}
addOrReplace(best)
```

`currentDevice` is captured **once** at the top of `postTransaction()`. Inside `PersistedStorageTransaction
ScopeImpl.removeDevice(id)` (lines 53–66), if the removed `id == settings.currentSelectedDeviceId`, the
implementation already sets `currentSelectedDeviceId = null`. The hook's later `setCurrentDevice(best)` then
re-assigns it — but only if the captured snapshot's `currentDevice?.uniqueId == device.uniqueId`. If the
current device is updated by an earlier hook (e.g. `AlwaysActiveHook` runs *first* due to its `HIGH`
priority), the captured `currentDevice` is stale.

Because hooks run in priority order (lowest first per `sortedBy`, see `FDevicePersistedStorageImpl` line 41,
HookPriority enum order is LOW=0, NORMAL=1, HIGH=2), `RemoveDuplicateCloudHook (LOW)` runs first, then
`RemoveDuplicateHardwareIdHook (NORMAL)`, then `AlwaysActiveHook (HIGH)`. Within the duplicate-removal
hooks, `currentDevice` is captured before any `removeDevice` calls, but `removeDevice` itself nulls
`currentSelectedDeviceId` if it matches. The hook then calls `setCurrentDevice(best)` — using the *old*
captured value — and immediately following it, `addOrReplace(best)`. This is fine for the simple "duplicate
of the current device" case, but consider:

- The current device has `uniqueId = X` with no duplicates of itself.
- Two **other** devices share `hardwareId` with each other, neither is X.
- The hook deduplicates them. It calls `removeDevice` for one of them. `removeDevice` does **not** touch
  `currentSelectedDeviceId` because the removed id is not X. Fine.
- But there's a separate scenario: when the duplicate set *includes* the current device but `best` is a
  *different* device. The hook captures `currentDevice = X`. Then loops, removes X (because X is not
  best.uniqueId). `removeDevice(X)` nulls the current selection. The hook then calls `setCurrentDevice(best)`
  to recover. Good. But `best = mergeBBIfEmpty(best, X)` runs *after* the recovery `setCurrentDevice(best)`,
  so the persisted current points to a `best` that is *missing* the merged transports from X — the
  subsequent `addOrReplace(best)` writes the merged version, but `setCurrentDevice` was called with the
  pre-merge value. Because `setCurrentDevice` only stores the `uniqueId`, the divergence is harmless *here*,
  but the order of operations is fragile and a subtle refactor (e.g. `setCurrentDevice` storing the whole
  object) would silently break.

A more concrete bug: `addOrReplace(best)` is called only after the inner `forEach`. Until then, the hook has
removed all duplicates of `best` from `settings.devices`, leaving the persisted state with `best` itself
removed if `best == settings.devices[i]` was the *first* found and was matched by a later iteration's
`device`. Because `forEach` skips `best.uniqueId`, this is avoided — but `best = mergeBBIfEmpty(...)` mutates
`best` to a **new** object with a different identity. The subsequent loop iterations still skip the original
`best.uniqueId`, but the final `addOrReplace(best)` writes the merged version. If the merge produces a
`uniqueId` change (it doesn't today, but the API allows it via `copyTransports(uniqueId)`), the original
`best` is dropped. Defensive coding would `setCurrentDevice(best)` *after* the loop and the `addOrReplace`.

## Repro

Difficult to trigger with the current `mergeBBIfEmpty` (which preserves `uniqueId`), but the ordering is a
correctness landmine for any future refactor.

## Impact

- Today: hard to hit, but `setCurrentDevice` is invoked with a pre-merge value in the duplicate-of-current
  case.
- Future regression: any change to `mergeBBIfEmpty` that returns a new uniqueId silently loses devices.

## Suggested fix

- Refactor: collect all duplicates into `best`, then in a single pass do `removeDevice(other.uniqueId)` for
  each other and finally `addOrReplace(best)` followed by `setCurrentDevice(best)` if needed. This separates
  observation from mutation.
- Add a unit test for the "current device is among the duplicates and not the chosen `best`" case.
