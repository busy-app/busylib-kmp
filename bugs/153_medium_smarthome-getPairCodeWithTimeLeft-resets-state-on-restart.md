# `getPairCodeWithTimeLeft()` emits `null`, ignores Smart Home pairing failures, and silently consumes throttler tokens

## Severity
medium

## Type
broken-feature

## Files
- `components/bridge/feature/smarthome/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/smarthome/impl/FSmartHomeFeatureApiImpl.kt` (lines 90–109)

## Summary
`getPairCodeWithTimeLeft()` is a `flow` that:

1. Always emits `null` first per outer iteration. Combined with the outer `while (currentCoroutineContext().isActive)` loop, every time a previous pair-code expires the consumer transitions through `null` again. UI consumers expecting "loading once at the start" instead see flicker each minute as new codes are minted.
2. Calls `getPairCode()` (which hits `POST /api/smart_home/pairing`) every time a code expires. If the device is unresponsive, `exponentialRetry { getPairCode().toKotlinResult() }` will loop forever inside the throttler, blocking other RPC.
3. There is no error surface. A repeatedly failing pair-code request just hangs the flow at the post-`null` state.

## Repro
1. Subscribe to `getPairCodeWithTimeLeft()`.
2. Wait for the first pair-code; let the timer expire.
3. Watch the flow re-emit `null` at the start of the next outer loop iteration — the UI's QR view briefly disappears.
4. Pull the network — `exponentialRetry` retries forever, the consumer sits at `null` indefinitely with no way to distinguish loading from failure.

## Root Cause
```kotlin
return flow {
    while (currentCoroutineContext().isActive) {
        emit(null)                                                     // re-emits null every cycle
        val pairCode = exponentialRetry { getPairCode().toKotlinResult() }
        do {
            ...
        } while (timeLeft > 0.seconds)
    }
}.wrap()
```
- `emit(null)` is unconditional.
- `exponentialRetry` defaults to `Long.MAX_VALUE` retries with no timeout.
- The `do/while` loop's `timeLeft` reads a `val` declared inside the loop; the `while` condition references the most-recent assignment, which is acceptable Kotlin semantics but easy to misread.

## Impact
- UI flicker on QR-code refresh.
- No error visibility for repeated pair-code RPC failures.
- Throttler token bucket dominated by retries, starving other RPC.

## Suggested Fix
- Only `emit(null)` on the very first iteration, then preserve the previous `BsbMatterCommissioningTimeLeftPayload` while waiting for the next code (or transition through a dedicated `Refreshing` sub-state).
- Cap `exponentialRetry`'s retries (e.g. 5) and surface failure via a sealed state (e.g. `Loading | Loaded(BsbMatterCommissioningTimeLeftPayload) | Error(Throwable)`).
- Add a unit test that walks through the timer expiry transition and asserts no `null` is emitted between codes.
