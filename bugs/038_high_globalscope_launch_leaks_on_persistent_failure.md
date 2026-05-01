# `onInternalDisconnect` `globalScope.launch` can pile up on rapid disconnect/reconnect

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:118-125`

## Summary
Every `Disconnected` event, every `onConnectError`, and every uncaught exception in the holder's
scope causes `onInternalDisconnect` to issue a fresh `globalScope.launch { withLock { ... } }`. The
launches are individually short-lived, but if the device is in a flapping state (e.g., BLE
connection that succeeds → fails → succeeds → fails repeatedly, or a ping-pong between LAN/Cloud),
the orchestrator never shed-loads.

Because:
- `Disconnected` is fired *both* by the transport listener path (line 94) *and* by the
  `invokeOnCompletion` in `FDeviceHolder` (line 102-110), each disconnect produces *two*
  launches.
- The `exceptionHandler` path (line 108-113) and `onConnectError` (line 102-107) also queue
  launches, with overlapping responsibilities.

In a flapping scenario, the queue grows faster than it drains, *especially* if the mutex is held
for a long time (for instance during a slow `disconnect()` that itself awaits the inner
`scope.coroutineContext.job.cancelAndJoin()`).

A concrete failure: the queue itself does not leak unbounded memory under normal conditions
(because each launch eventually gets to run), but the back-pressure means that a fresh
`connectIfNot(...)` issued by the user is serialized *behind* a backlog of stale cleanup launches.
The user perceives the orchestrator as "frozen" for several seconds during a flapping period.

## Reproduction / scenario
1. Device flaps: every 100ms the transport reports `Disconnected → Connecting → Connected →
   Disconnected ...`.
2. Each disconnect queues 2 `globalScope.launch` (transport listener + `invokeOnCompletion`).
3. After a few flaps, the mutex queue holds 10–20 pending tasks.
4. User taps "disconnect" / "switch device" → their `connectIfNot` joins the back of the queue.
5. The user observes a multi-second delay before the orchestrator responds.

## Why it happens
- No deduplication of disconnect events for the same holder.
- No bound on how many stale-cleanup launches can be queued.
- `invokeOnCompletion` and `transportConnectionListener` both emit Disconnected for the same
  underlying event (see `high_duplicate_disconnect_callback_from_invokeoncompletion.md`).

## Impact
- Perceived UI hangs during transport flapping.
- Log spam ("Tried to disconnect not current device, skip" repeated dozens of times).
- Memory churn from per-event coroutine allocation.

## Suggested fix
- Deduplicate Disconnected events at the holder level (`AtomicBoolean disconnected`).
- Coalesce queued cleanups: track an `isPendingDisconnect` flag for each holder; only launch one
  cleanup per holder.
- Or use a single, long-lived disconnect-handler coroutine that consumes a `Channel<Pair<Holder,
  PostAction>>`, instead of one launch per event.
