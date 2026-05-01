# Listener receives `Connecting` twice on `connect()`

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/CombinedConnectionApiImpl.kt:24-41`
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/FCombinedConnectionApiImpl.kt:73-97`

## Summary
`CombinedConnectionApiImpl.connect` invokes `listener.onStatusUpdate(Connecting(allTypes))` *and* then constructs `FCombinedConnectionApiImpl`, whose init block immediately starts a status-collection job that re-emits the current snapshot via `listener.onStatusUpdate(...)`. The current snapshot is also `Connecting(allTypes)` because each `AutoReconnectConnection.stateFlow` defaults to `Connecting`. The listener thus receives `Connecting` twice, with no value change in between.

If the listener is, e.g., translating connection events to a UI animation, this can cause it to reset / replay the spinner; if the listener increments a counter or logs unique transitions, it sees a duplicate.

## Repro
Pass an instrumented `FTransportConnectionStatusListener`:

```kotlin
val statuses = mutableListOf<FInternalTransportConnectionStatus>()
combinedApi.connect(scope, config, { statuses.add(it) }, builder)
advanceUntilIdle()
println(statuses)
// [Connecting(...), Connecting(...)]
```

## Root Cause
- `CombinedConnectionApiImpl` emits a manual `Connecting` to gate its callers.
- `FCombinedConnectionApiImpl.startCollectTransportStatusUpdateJob` does not skip the initial value of its merged flow; the first emission is identical to the manual one.

## Impact
- Listener observes a duplicate event.
- May confuse downstream debouncing / event-counting logic.

## Suggested Fix
Drop the manual emission in `CombinedConnectionApiImpl.connect` (the constructor's init block already publishes the initial `Connecting`). Or, in `FCombinedConnectionApiImpl`, use `distinctUntilChanged()` against the most-recently-emitted status to the listener (the `getCurrentConnectionSnapshotFlow` already calls `distinctUntilChanged` on the snapshot, but the manual emission and constructor-emission carry the same status).
