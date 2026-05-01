# Orchestrator subscribes to `subscriberCountsFlowWithDebounce` twice — duplicate `invalidateSubscribers`

## Severity
medium

## Type
broken-feature

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/orchestrator/CloudWebSocketOrchestratorApiImpl.kt` (lines 49-96)

## Summary
`subscriberCountsFlowWithDebounce` is a cold flow (val on top of `MutableStateFlow.debounce(1.seconds)`). It is consumed in two places in the same upstream graph:

1. Inside `getWSFlow()`:
   ```kotlin
   subscriberCountsFlowWithDebounce
       .map { it.isNotEmpty() }
       .distinctUntilChanged()
       .flatMapLatest { ... }
   ```
2. Inside `getWSEventsFlow()`:
   ```kotlin
   combine(
       subscriberCountsFlowWithDebounce,
       getWSFlow(), // also uses subscriberCountsFlowWithDebounce internally
   ) { ... }
   ```

Because debounce is cold, each subscription has its own debounce timer. A single
underlying change in `subscriberCountsFlow` therefore propagates twice (once per
collector) into the combine, and `combine` itself collects both sources independently.

Effects:
- Each combine fire calls `activeWebSocketHolder.invalidateSubscribers(...)` — the
  holder mutex serializes these but they are still wasteful.
- Subtle timing skew: the two debounce timers can fire on different ticks if the
  scheduler is shared between many coroutines, so `combine` can briefly see
  `subscriberCounts = NEW` paired with a `webSocket` that was derived from
  `subscriberCounts = OLD` (or vice versa). With `Lazily` sharing on
  `wsEventSharedFlow`, the upstream may emit a `flowOf(events)` from a stale
  pairing.
- `getWSFlow()` and `wsEventSharedFlow` both subscribe to
  `webSocketApi.getWSInternalFlow()` — fine since that one is `shareIn`'d.

## Repro
- Add a counter inside `invalidateSubscribers` and subscribe one consumer to
  `getEventsFlow(barId)`. After the 1s debounce fires, the counter increments by 2
  (or more, depending on combine's emission semantics).

## Root Cause
- `subscriberCountsFlowWithDebounce` is not `shareIn`'d. The author likely expected
  debounce to share its state, but `Flow.debounce` is cold.

## Impact
- Wasted CPU / extra holder.invalidateSubscribers calls (~2x).
- Race window where the WS-flow source and the subscriberCount source disagree on
  the latest value during a single combine emission.

## Suggested Fix
```kotlin
private val sharedSubscriberCounts = subscriberCountsFlow
    .debounce(1.seconds)
    .shareIn(scope, SharingStarted.Eagerly, replay = 1)
```
…and use `sharedSubscriberCounts` in both `getWSFlow()` and `getWSEventsFlow()`.
That guarantees a single debounce timer and a single shared latest value.
