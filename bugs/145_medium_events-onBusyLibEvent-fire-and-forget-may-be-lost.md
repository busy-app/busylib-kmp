# `FEventsFeatureApi.onBusyLibEvent` is fire-and-forget; emissions may be lost

## Severity
medium

## Type
broken-feature

## Files
- `components/bridge/feature/events/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/events/impl/FEventsFeatureApiImpl.kt` (lines 36–40)

## Summary
`onBusyLibEvent(event)` does `scope.launch { busyLibUpdateEventFlow.emit(event) }`. `busyLibUpdateEventFlow` is a plain `MutableSharedFlow<BusyLibUpdateEvent>()` with default `replay = 0` and `extraBufferCapacity = 0`. `MutableSharedFlow.emit` suspends until every active collector has received the event. With no buffer and no replay:

1. If no collector is active when `emit` runs, the launched coroutine *suspends forever* (or until the scope dies). The fire-and-forget caller has no idea.
2. Producers that immediately invoke `onBusyLibEvent` after a state change (e.g. `setVolume`, `setBrightness`, `setDeviceName` in `FSettingsFeatureApiImpl`) leak coroutines if the events feature has no collector at that moment (e.g. if the consuming feature is created lazily later).
3. Order is not preserved across `launch`-ed emits; rapid back-to-back `onBusyLibEvent` calls can be reordered by the dispatcher.

## Repro
1. Create the events feature, but no other feature subscribes yet.
2. Call `onBusyLibEvent(BusyLibUpdateEvent.DeviceName("foo"))`.
3. Inspect `scope.coroutineContext.job.children` — there is a launched coroutine suspended on `emit`.
4. Repeat 10× quickly. Observe 10 stuck child coroutines accumulating until a collector finally appears (or scope is cancelled).

## Root Cause
- `MutableSharedFlow` defaults: `replay = 0, extraBufferCapacity = 0, onBufferOverflow = SUSPEND`.
- `scope.launch { ... emit(...) }` does not propagate failure or completion to the caller.

## Impact
- Optimistic UI updates (the pattern `setX` → `onBusyLibEvent(X)`) silently drop the optimistic event when no other feature is collecting events at that exact moment.
- Cancellation of the parent scope cancels the emit-launches; events on the wire just before disconnect are lost.
- Memory: under high event burst, accumulated launched-but-suspended coroutines hold references to event payloads (esp. `BusyLibUpdateEvent.Frame` carrying ByteArrays).

## Suggested Fix
- Use a buffered `MutableSharedFlow` with `extraBufferCapacity = N` (e.g. 32) and `onBufferOverflow = DROP_OLDEST`.
- Or use `tryEmit` and drop with a warning rather than `launch { emit }`.
- Or back the in-process bus with a `Channel<BusyLibUpdateEvent>(BUFFERED).consumeAsFlow()` to preserve ordering.
- Add a unit test that calls `onBusyLibEvent` with no collectors and verifies a subsequent collector receives the value (or that the framework documents drop semantics).
