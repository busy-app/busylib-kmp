# `onInternalDisconnect.postAction` writes Disconnected to the *old* listener after a new listener has been installed

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:92-125`

## Summary
`onInternalDisconnect(deviceHolder, postAction)` schedules `globalScope.launch { withLock { ... }
}`. The `postAction` lambda captures `localTransportListener` (the one created during the
`connectIfNot` that built the holder). The launch waits for the mutex; in the meantime, the user
may call `connectIfNot(deviceB)`, which acquires the mutex first, swaps the
`transportListenerFlow` to a new `localTransportListener`, and releases the mutex. The queued
launch then runs and:

1. Detects `currentDevice !== deviceHolder` → skips disconnect cleanup (correct).
2. Still calls `postAction()`, which writes `Disconnected` (or `Connected/Connecting/...`) to the
   **old** listener.

The old listener's `MutableStateFlow` is no longer wired to the public state (it was replaced),
so the spurious write is silent – *if* the new listener has already been emitted to
`transportListenerFlow` before the launch's `flatMapLatest` resubscribes. But: the launch can
race with an even-newer `connectIfNot(deviceC)` that hasn't yet emitted; the `flatMapLatest` may
be subscribed to deviceB's listener at the moment the launch fires, but the postAction writes to
deviceA's listener (no harm). However, when the public state pipeline is processing values
in-flight (say, a slow downstream collector), it can momentarily observe deviceA's
state-flow value before being unsubscribed, depending on scheduling. The result is occasional
out-of-order events visible to consumers.

A more concrete and worse scenario:

If the user calls `connectIfNot(deviceA)` → exception fires (postAction = onErrorDuringConnect)
→ launch queued. Then user calls `connectIfNot(deviceA)` *again* (same uniqueId). Orchestrator
sees `currentDevice` is still holderA (not yet cleaned up by the queued launch) and goes to
`tryToUpdateConnectionConfig` → fails (holder is dead) → falls through to disconnectInternalUnsafe
→ `disconnect()` on the dead holder (which can throw or be a noop). New holder is created with a
*new* `localTransportListener_2`. Mutex released. Now the queued launch from the *original*
attempt finally acquires the mutex. It detects `currentDevice !== originalHolderA` → skip
disconnect. It calls `postAction_1()` → writes ERROR_UNKNOWN to listener_1 → no subscribers,
silently lost.

So the visible failure depends on subtle timing.

## Reproduction / scenario
1. `connectIfNot(deviceA)` – exception fires on connect error.
2. Inside the callback: `onInternalDisconnect(holderA, postAction = { listener_1.onErrorDuringConnect(...) })`.
3. `globalScope.launch { withLock("disconnect_internal") { ... } }` queues.
4. User immediately calls `connectIfNot(deviceA)` again. Orchestrator acquires mutex first
   (caller's coroutine is racing with the queued globalScope launch; depending on dispatchers,
   either may win).
5. If the caller wins: orchestrator does its thing, builds holderA' with listener_2, releases.
6. Queued launch wins lock. Cleanup is skipped (holder mismatch). postAction_1 still runs,
   updating listener_1. Effect: noisy but invisible.

## Why it happens
- The author mixes "do cleanup *and* notify the listener" together in `postAction`. When cleanup
  is skipped (because the holder is no longer current), the notification is *also* irrelevant –
  but the code still runs it.

## Impact
- Listener state writes that don't reach any subscriber (wasteful but not incorrect).
- Difficult-to-trace event ordering in logs because the same listener can receive late updates
  long after it has been replaced.
- Potential cross-contamination if the design ever switches to a SharedFlow-based listener model
  (where late writes *would* be observed).

## Suggested fix
Merge the cleanup decision and the postAction:

```kotlin
private fun onInternalDisconnect(
    deviceHolder: FDeviceHolder<*>,
    postAction: () -> Unit
) {
    globalScope.launch {
        withLock(mutex, "disconnect_internal") {
            if (currentDevice !== deviceHolder) {
                info { "Stale disconnect for retired holder, ignoring" }
                return@withLock
            }
            disconnectInternalUnsafe(deviceHolder)
            postAction()
        }
    }
}
```

so `postAction` is only called when the holder is still current.
