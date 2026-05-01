# `CombinedMetaInfoApiImpl` and `FCombinedStreamingApiImpl` resolve queries against an undefined transport order

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/metakey/CombinedMetaInfoApiImpl.kt:23-48`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/streaming/FCombinedStreamingApiImpl.kt:21-31`

## Intended behavior (per project owner)
Transport priority is **LAN > Cloud > BLE**. Both meta-info reads and streaming subscriptions should resolve against the highest-priority connected transport — deterministically, not by input order or by emission timing.

## Summary
Both `CombinedMetaInfoApiImpl.delegates` and `FCombinedStreamingApiImpl.delegates` derive their list from `connectionPool.get()` and then:

- meta info: returns `results.find { it.isSuccess }` — the first **list-order** success.
- streaming: returns `currentDelegates.firstOrNull()?.getEvents()` — the first **list-order** transport.

`SharedConnectionPool.sharedState` is built from `connectionsFlow.flatMapLatest { ... }.combine()`. The order is the order of `connectionsFlow` (i.e. matches `connections.value`), but only **after the most recent emission has updated all positions** — `combine` re-emits eagerly whenever any single source emits, so a stale list with one updated value is visible. Worse, because `flatMapLatest` resets the inner emission for the changed connection each time, the effective set of "which delegates implement `FTransportMetaInfoApi`/`FStatusStreamingApi`" can briefly look different from the steady state.

There is also no transport-priority awareness here. If both BLE and Cloud answer the same key successfully, the result silently depends on which transport's `getEvents()` / `get()` happened to update last.

## Repro
1. Two transports both expose meta info. Battery flows: BLE returns `42`, Cloud returns `41`.
2. Subscribe to `metaInfoApi.get(BATTERY_LEVEL)` and observe values.
3. Reconnect/redial cycle on Cloud causes the flow position to "win" — the consumer suddenly sees `41` even though BLE is the higher-quality source.

## Root Cause
- `find { it.isSuccess }` on `combine(flows)` results uses iteration order of the input `flows`, which mirrors `currentDelegates`, which mirrors `connectionPool.get()`, which mirrors the order from `connections.value`. That is OK if `connections.value` actually corresponds to the configured priority — but `FCombinedConnectionApiImpl` allows arbitrary configurations and never sorts by transport priority.
- For streaming, `firstOrNull()` blindly picks index 0 — equally arbitrary.

## Impact
- Meta info (`DEVICE_NAME`, `BATTERY_LEVEL`, etc.) silently flips between transports.
- `getEvents()` may be served by a slow transport even when a faster one is available.
- For BUSY Bar specifically, BLE and Cloud reporting different values for the same key (e.g. firmware-version skew during OTA) will produce flapping output.

## Suggested Fix
Sort `currentDelegates` by an explicit transport priority before `find`/`firstOrNull`. Example:

```kotlin
private val delegates = connectionPool.get().map { list ->
    list
        .mapNotNull { (it.status as? Connected)?.takeIf { c -> c.deviceApi is FStatusStreamingApi } }
        .sortedByDescending { transportPriority(it.connectionTypes) }
        .map { it.deviceApi as FStatusStreamingApi }
}
```

Apply the same pattern in `CombinedMetaInfoApiImpl`. Even better, expose the priority via configuration so callers can tune which transport "wins" for meta/streaming/HTTP.
