# `deviceVersionFlow` swallows errors and silently completes; replay caches no value

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/feature/info/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/info/impl/FDeviceInfoFeatureApiImpl.kt` (lines 48–59)

## Summary
`deviceVersionFlow` is implemented as a one-shot `flow { emit(...) }.shareIn(scope, SharingStarted.Lazily, 1)`. Although the inner work is wrapped in `exponentialRetry`, the chosen `SharingStarted.Lazily` keeps the upstream collector alive only until the first emission — which is fine in the success case. But:

1. If the connection drops mid-`exponentialRetry` and the parent scope is cancelled, the upstream completes without emitting anything. New subscribers block forever; the replay cache is empty.
2. `getStatusFirmware()` failures inside `exponentialRetry` retry forever (default `Long.MAX_VALUE`), so on a permanently-broken connection the flow simply never produces a value, but never errors out either — `WrappedFlow` consumers hang.
3. After a successful emission, `deviceVersionFlow` never re-fetches even after a full reconnect (because the parent feature scope is recreated, and so is the impl, but consumers have to redo the subscription dance to notice).

## Repro
1. Disconnect the device while `getStatusFirmware()` is mid-retry (force network failure).
2. Subscribe to `deviceVersionFlow` from a UI consumer — collect never receives anything.
3. Reconnect and observe whether the new subscriber gets the new device's version: the replay cache from the old impl is gone (impl recreated), but the new impl's subscriber chain has the same deficiency.

## Root Cause
- `flow { emit(...) }.shareIn(scope, Lazily, 1)` is a one-shot pattern. The `replay = 1` cache only ever holds *one* value across the lifetime of the impl. There is no resubscription / refresh on reconnect of a subprocess.
- The upstream re-throws exceptions (`exponentialRetry` ultimately throws), but `shareIn` propagates that to subscribers and stops; `Lazily` doesn't restart.
- The exposed flow is `WrappedFlow<BsbBusyBarVersion>`, not `WrappedStateFlow`, so consumers cannot detect "still waiting" vs "no value".

## Impact
- "About"/"Device info" screens stay blank if the very first call to `getStatusFirmware` is slow or fails permanently.
- Even after recovery, late subscribers may still see nothing because the `Lazily` upstream completed with the failure.
- No way for callers to distinguish loading from error.

## Suggested Fix
- Promote `deviceVersionFlow` to a `WrappedStateFlow<BsbBusyBarVersion?>` (or expose a sealed `Resource<BsbBusyBarVersion>`).
- Use `SharingStarted.WhileSubscribed(stopTimeout, replayExpiration)` and let the upstream restart on resubscription.
- Or just make `getDeviceVersion(): suspend CResult<BsbBusyBarVersion>` and let consumers handle the state machine.
- Cap retries inside `exponentialRetry` (e.g. 5) and surface failure rather than retrying for the lifetime of the connection.
