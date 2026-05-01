# `CloudFetcher` applies a 1-second `debounce` to the WebSocket handle flow

## Type
broken-feature

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/utils/CloudFetcher.kt` lines 41-54

## Summary

```kotlin
wsApi.getWSFlow()
    .debounce(1.seconds)
    .flatMapLatest { ws -> if (ws == null) emptyFlow() else getBarsFlow(principal, ws.getEventsFlow()) }
```

The `debounce(1.seconds)` is applied on the *handle* flow, not on the
event flow. Two real consequences:

1. **First-second event loss.** When the WS flips `null -> WebSocket(...)`,
   subscription to `ws.getEventsFlow()` is delayed by 1 s. Any event the
   server pushes during that 1 s window is lost (events are not buffered by
   the watchers — see `CloudWebSocketApi.getEventsFlow()` consumers).
2. **Reconnect coalescing causes silent state drift.** If the WS handle
   flickers `WS_A -> null -> WS_B` faster than 1 s (server hiccup, app
   foregrounding, rapid network change), `debounce` emits only the *last*
   value. `flatMapLatest` then subscribes to `WS_B.getEventsFlow()` but
   the seed REST call inside `getBarsFlow` is *not re-run* (it is a one-shot
   inside the same flow). The previously seeded `barsMap` from `WS_A`
   remained valid, but if the user-cloud bars list changed during the
   reconnect, the local cache is stale until the next user-driven principal
   or network change.

## Repro (Scenario 1, event loss)

1. User logs in, network online. `wsApi.getWSFlow()` emits `WebSocket(...)`.
2. `debounce(1.seconds)` waits 1 s before emitting downstream.
3. During that 1 s the cloud emits `WebSocketEvent.NameChangeEvent` for an
   existing device.
4. `flatMapLatest` subscribes to `getEventsFlow()` only after the debounce.
   The earlier name change is gone.
5. Local device list still shows the old name.

## Repro (Scenario 2, fast reconnect coalesces away the seed)

1. WS connects (`WS_A`), seed REST fetches bars `[A, B]`.
2. WS drops, `wsApi.getWSFlow()` emits `null`. Within < 1 s, WS reconnects
   as `WS_B`.
3. `debounce` cancels the `null` emission, emits `WS_B`.
4. `flatMapLatest` cancels the inner flow tied to `WS_A`, opens a new flow
   tied to `WS_B`. The flow's first action is the REST seed — which will
   refresh the list, *but* see the related bug
   `high_cloud-fetcher-ws-events-lost-on-bars-list-failure` for what happens
   when that seed fails.

## Root Cause

`debounce` here looks intended to suppress a `null` blip during reconnect
("ignore brief drops"), but the chosen operator drops *every* leading-edge
emission, not just the falsy ones. The correct primitive for "ignore brief
nulls" is something like `mapLatest { delay(1.seconds) ; it }` for nulls
only, or a stateful `transformLatest` that emits non-null immediately and
delays a null transition.

## Impact

- WS events delivered in the first second after WS-up are silently lost.
- Rapid reconnects can mask a `null` transition, leaving subscribers tied to
  a freshly opened socket while the inner state machine assumes nothing
  changed.

## Suggested Fix

1. Remove the blanket debounce. Replace with selective handling, e.g.:
   ```kotlin
   wsApi.getWSFlow()
       .transformLatest { ws ->
           if (ws == null) {
               delay(1.seconds) // tolerate transient drops
               emit(null)
           } else {
               emit(ws)
           }
       }
       .flatMapLatest { ws -> ... }
   ```
2. If the goal is rate-limiting subscription thrash, use `sample` /
   `conflate` on the handle but ensure that whatever handle survives, its
   event flow is subscribed without delay.
3. Add tests asserting that an event emitted within 100 ms of WS-up reaches
   storage.
