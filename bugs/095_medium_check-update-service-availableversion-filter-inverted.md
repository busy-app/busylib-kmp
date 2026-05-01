# `CheckUpdateService` only re-issues update checks when the previous check has already produced a version

## Type
broken-feature

**Severity:** medium

**Files:**
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/updater/service/CheckUpdateService.kt` (lines 36–73)

## Summary

`CheckUpdateService.onLaunch()` filters the update-status flow with:

```kotlin
.filter { status -> status?.check?.availableVersion.isNullOrEmpty() }
```

Then a second `.filter` only forwards `null` and `NONE` check statuses. The first filter is meant to read
"only re-trigger a check if no version has been computed yet," but combined with the second filter it
becomes a degenerate condition that fires almost continuously while no update is available. Each emission
that satisfies it goes through `flatMapLatest { fFeatureProvider.get<FRpcFeatureApi>() }` and ends in a real
`startUpdateCheck()` call.

Because `updateStatusFlow` is a `WrappedSharedFlow` that re-emits its cached value on resubscription, every
reconnection / orchestrator restart triggers another `startUpdateCheck()` even though nothing has changed,
hammering the device with redundant `/api/update/check` requests.

The `.distinctUntilChanged()` on line 56 only de-dupes against the immediately previous value but does not
prevent re-triggering after subscription churn.

## Repro

1. Pair a device, leave it idle.
2. Toggle Wi-Fi a few times so the orchestrator re-publishes the feature.
3. Observe `/api/update/check` being called on every churn cycle, even though nothing has changed.

## Root cause

The intent of the filter chain is unclear, but at minimum:

- `isNullOrEmpty()` returns `true` for `null` versions, which then proceeds — yet for `null` versions the
  check status is also being filtered, so we forward them for any of `null` and `NONE`.
- Re-subscription is treated as a change.

## Impact

- Excess RPC traffic, increased device wake-ups, faster battery drain on the BUSY Bar.
- Mild server load on the cloud relay.

## Suggested fix

- Track a `MutableStateFlow<CheckResult>` and only fire `startUpdateCheck()` when transitioning from
  "unknown" to "we want a check". Don't re-fire on resubscription.
- Replace the two-step `.filter { isNullOrEmpty() } / .filter { status?.check?.status in [null, NONE] }` with
  a single explicit predicate:

```kotlin
.filter { status -> status?.check?.status == BsbCheckResult.NONE && status.check.availableVersion.isEmpty() }
```
