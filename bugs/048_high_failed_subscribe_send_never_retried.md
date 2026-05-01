# Failed `SubscribeState`/`UnsubscribeState` send is never retried

## Severity
high

## Type
lack-of-feature

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/orchestrator/ActiveWebSocketHolder.kt` (lines 24-90)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/orchestrator/CloudWebSocketOrchestratorApiImpl.kt` (lines 72-96)

## Summary
`ActiveWebSocketHolder.invalidateSubscribers` only adds an id to `activeSubscriptionsSet`
**after `safeSend(...).onSuccess { ... }`**. If `safeSend` fails (timeout after 5s,
serialization error, transient I/O), the id is **not** considered active — but the
combine source (`subscriberCounts × ws`) only refires on a real change to either side.
The orchestrator never schedules a retry, so the consumer's
`getEventsFlow(cloudId).collect { ... }` will hang indefinitely with no events arriving
for that bar even though the WebSocket is healthy.

The same applies to `UnsubscribeState`: a failed unsubscribe leaves the id in
`activeSubscriptionsSet`, so the server keeps streaming events for a bar nobody
subscribes to anymore — wasting bandwidth and (more importantly) blocking subsequent
re-subscribes (the diff `shouldBeActive.minus(activeSubscriptionsSet)` will be empty
when the user re-subscribes to the same id).

## Repro
1. Configure the WS so `send(SubscribeState(...))` throws once (e.g. simulate a
   `withTimeout` expiry by stalling the underlying socket for >5s).
2. Subscribe to `getEventsFlow(barId)` from a consumer.
3. Restore WS `send` to working state but do NOT change anything else.
4. Emit a server-side event for `barId` over the same WS.
5. Observe: consumer never receives the event. `subscriberCountsFlow` holds count = 1
   so `combine` does not re-fire; activeSubscriptionsSet is empty so the next combine
   trigger (e.g. another `cloudId` subscribing) recomputes diff and **only** subscribes
   the new id, leaving the original still unknown to the server.

## Root Cause
- The retry policy is "retry whenever combine fires"; combine only fires on subscriberCount
  changes or on WS swap. Transient send failures don't satisfy that.
- `safeSend` swallows the failure (logs it and returns) — by design ignores cancellation —
  but the orchestrator never observes the failure.
- The state machine optimistically updates only on success, so once a retry path is
  entered, partial application leaves the holder in an inconsistent local view.

## Impact
- Silent loss of device state updates after any transient WS write hiccup.
- After unsubscribe failure, the next subscriber to the same id will never re-subscribe
  because `shouldBeActive.minus(activeSubscriptionsSet)` becomes empty.
- Orphaned subscriptions on the server side cost bandwidth.

## Suggested Fix
- Wrap `safeSend(...)` with `exponentialRetry { }` (from `core:ktx`) before treating
  it as final, OR
- On failure, signal the combine source to retrigger (e.g. emit through a
  `_retrySignal` `MutableSharedFlow<Unit>` that combine listens on), OR
- Periodically reconcile (every N seconds) by comparing
  `subscriberCountsFlow.value` vs `activeSubscriptionsSet` and re-issue the diff.
- Update `activeSubscriptionsSet` only after the server acknowledges (if there is an
  ack message) instead of on `send` success.
