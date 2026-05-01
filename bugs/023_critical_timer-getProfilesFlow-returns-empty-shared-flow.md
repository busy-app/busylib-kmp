# `FTimerFeatureApi.getProfilesFlow(slot)` always returns an empty flow that never emits

## Severity
critical

## Type
broken-feature

## Files
- `components/bridge/feature/timer/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/timer/impl/FTimerFeatureApiImpl.kt` (lines 48–50)
- `components/bridge/feature/timer/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/timer/api/FTimerFeatureApi.kt` (line 10)

## Summary
The public Timer feature exposes `getProfilesFlow(slot: BusyProfileSlot): WrappedSharedFlow<String>`, but the implementation completely ignores `slot` and returns a freshly-created `MutableSharedFlow<String>().wrap()` on every call. That `MutableSharedFlow` is never written to, has no replay, and is never connected to RPC or events. Every collector hangs indefinitely.

## Repro
```kotlin
val flow = fTimerFeatureApi.getProfilesFlow(BusyProfileSlot.BUSY)
flow.collect { profile -> /* never reached */ }
```

## Root Cause
```kotlin
override fun getProfilesFlow(slot: BusyProfileSlot): WrappedSharedFlow<String> {
    return MutableSharedFlow<String>().wrap()
}
```
- The `slot` parameter is unused.
- A new `MutableSharedFlow` is allocated on every call, so subscribers never share state and there is no upstream populating it.
- `FRpcBusyApi` does not expose a `getBusyProfile(slot)` endpoint, so the data source isn't even wired.

## Impact
- Any consumer (Swift via SKIE included) calling `getProfilesFlow` waits forever and silently sees no data.
- Worse: the API surface advertises this as supported, but the contract is unimplementable through the current interface.
- This is a public API that is part of the Maven and XCFramework artifacts, so external consumers hit it too.

## Suggested Fix
- Add a `suspend fun getBusyProfile(slot: String): Result<String>` to `FRpcBusyApi` and implement against `/api/busy/profiles/{slot}` (mirror of the existing `setBusyProfile`).
- Wire `getProfilesFlow(slot)` through `FEventsFeatureApi` if the firmware emits a per-slot profile change event, otherwise re-fetch via `exponentialRetry` and expose as a `SharedFlow` (using `shareIn(scope, ..., replay = 1)`).
- Until a real source exists, mark the API `@Deprecated("not implemented")` so consumers don't subscribe and stall.
