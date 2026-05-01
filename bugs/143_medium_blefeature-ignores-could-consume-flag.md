# `FBleFeatureApiImpl` always passes `ignoreCache=false` to `getBleStatus`, ignoring the `couldConsume` hint from the events feature

## Type
broken-feature

**Severity:** medium

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/feature/ble/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/ble/api/FBleFeatureApiImpl.kt` (lines 34-43)
- contract: `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/feature/events/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/events/api/FEventsFeatureApi.kt` (lines 33-49)

## Summary

The events feature's `get(scope, initial, mapper)` invokes the `initial`
lambda with a `couldConsume: Boolean` flag indicating whether the most
recent consumable update event was already consumed by another feature.
Other consumers (e.g. status feature) use this to decide whether the
RPC fetch should bypass the cache (`ignoreCache = !couldConsume` or
similar).

`FBleFeatureApiImpl` ignores the parameter:

```kotlin
private val bleStatusSharedFlow = fEventsFeatureApi.get(
    scope = scope,
    initial = {
        rpcFeatureApi.fRpcBleApi
            .getBleStatus(false)        // ← always uses cache
            .onFailure { … }
            .map { response -> response.toEvent() }
    },
    mapper = { flow -> flow.map { it.toPublic() } }
).asFlow().wrap()
```

The lambda parameter `couldConsume` is shadowed (the lambda has no named
parameter, so it's `it`), and the call site hardcodes `false`. So:

- When the events feature can no longer consume an event (`couldConsume ==
  false`), the BLE feature still fetches with cache enabled, returning
  stale data.
- When the events feature could consume (no other reader reset it),
  `getBleStatus(false)` again uses cache. So the flag is dead.

The visible symptom is that `getBleStatus()` may emit a stale BLE status
right after a state change reported by another feature.

## Reproduction

1. Subscribe to `getBleStatus()` via the public API.
2. Trigger a remote BLE state change (e.g. enable advertising via the bar's
   web UI).
3. The shared flow emits the cached value rather than refetching, so the
   consumer observes `Disabled` for up to one cache TTL after the device
   reports `Enabled`.

## Root cause

The lambda parameter from `get()` was forgotten when wiring the BLE
feature, leaving the cache logic inert.

## Impact

- Potential stale `FBleStatus` for the duration of `ObjectCache`'s TTL.
- Confusing UI flicker when subscribers correlate this stream with other
  events.

## Suggested fix

```kotlin
private val bleStatusSharedFlow = fEventsFeatureApi.get(
    scope = scope,
    initial = { couldConsume ->
        rpcFeatureApi.fRpcBleApi
            .getBleStatus(ignoreCache = !couldConsume)
            .onFailure { error(it) { "Failed to get Ble status" } }
            .map { response -> response.toEvent() }
    },
    mapper = { flow -> flow.map { it.toPublic() } }
).asFlow().wrap()
```
