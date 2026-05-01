# Failover priority is non-deterministic when multiple transports share the same status priority

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/FCombinedConnectionApiImpl.kt:56-71`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/ConnectionSnapshotMerger.kt:7-50`

## Summary
`getCurrentConnectionSnapshotFlow()` selects the active transport set by `groupBy(getPriority(status)).maxBy(priority)` and then calls `mergeSnapshots(...)` on that group. `mergeSnapshots` keeps `base = snapshots.first()` for the `Connected` payload (i.e. *which* transport's `deviceApi` and `scope` are exposed). When two transports are connected at the same time, "first" is determined by **iteration order of the `groupBy` value list**, which in turn comes from the order in which the underlying `combine` produced them — i.e. the order they finished emitting, **not** the configured `connectionConfigs` order.

There is no deterministic transport priority: which device api callers actually talk to via `Connected.deviceApi` (and indirectly via `getDeviceHttpEngine`, which is a separate engine but `mergeSnapshots`-derived state still drives metadata, capabilities, and streaming) can flip between two equivalently-prioritised transports based on timing.

## Repro
1. Combined config = `[BLE, LAN]`.
2. Both transports finish connecting; both emit `Connected`.
3. `mergeSnapshots` reports `Connected(scope = base.scope, deviceApi = base.deviceApi, types = [BLE, LAN])` — but `base` is whichever child's `flatMapLatest` happened to deliver first, not BLE.

## Root Cause
- `Map.maxBy { (priority, _) -> priority }` returns one of the entries with the maximum priority, but `groupBy` produces values in encounter order, and `mergeSnapshots` picks `snapshots.first()` from that list. The encounter order is the **emission order of `combine`**, not the configured order, so the choice is timing-dependent.
- `FCombinedConnectionApiImpl.connections` is a `StateFlow<List<...>>` that does preserve configured order, but the snapshot list in `SharedConnectionPool.sharedState` is also produced by `connections.map { ... }.combine()` — so the listed order matches `connections` only if `combine` always emits all flows in the same order. When upstreams change concurrently, `combine` emits in the order it receives the changes, not in source order. The downstream `groupBy { priority }` is keyed on priority but the value list inherits whatever order was passed into `mergeSnapshots`.

Concretely, even with deterministic source order, when you do `groupBy { priority }`, the entries with the highest priority can appear in any input-order, and `snapshots.first()` is the first of those — so if BLE (priority `Connected`) and LAN (priority `Connected`) are both in the highest bucket, and BLE happens to be index 1 in the value list while LAN happens to be at index 0 (because, say, only LAN's status changed in the latest combine emission), `base` becomes LAN.

## Impact
- HTTP requests, meta-info reads, and streaming events can land on the "wrong" transport with no observable rhyme or reason.
- A consumer expecting BLE-preferred behaviour silently uses Cloud, which is slower / costs more / has different security guarantees.
- Hard to debug because behaviour depends on observed wall-clock interleavings.

## Intended priority (per project owner)
**LAN > Cloud > BLE.** When multiple transports report `Connected` simultaneously, the selected `Connected.deviceApi` / `scope` / metadata source must be LAN if available, else Cloud, else BLE. The current implementation does not encode this — see the suggested fix below.

## Suggested Fix
Use the explicit transport priority `LAN > Cloud > BLE` as the tiebreaker:

```kotlin
private fun getStatusPriority(status: FInternalTransportConnectionStatus): Int = ... // existing
private fun getTypePriority(types: NonEmptyList<FInternalTransportConnectionType>): Int = ... // new

connectionsList
    .groupBy { getStatusPriority(it.status) }
    .maxBy { it.key }
    .value
    .sortedByDescending { getTypePriority(it.status.connectionTypes) } // or use configured order from `connections.value`
    .let(::mergeSnapshots)
```

Also reconsider `mergeSnapshots`'s "base = snapshots.first()" — it should pick a deterministic representative (e.g. the highest-typed transport).
