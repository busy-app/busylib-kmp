# `connectionLoop` performs suspending side-effects inside `combine`, may drop intermediate states

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/service/impl/src/commonMain/kotlin/net/flipper/bridge/connection/service/impl/FConnectionServiceImpl.kt` lines 60-91

## Summary

```kotlin
combine(
    flow = getExpectedState(),
    flow2 = orchestrator.getState()
) { expectedState, realState ->
    info { "expectedState: $expectedState, realState: $realState" }
    when (realState) {
        is FDeviceConnectStatus.Connected -> when (expectedState) {
            ...
            ExpectedState.Disconnected -> orchestrator.disconnectCurrent()
        }
        is FDeviceConnectStatus.Disconnected -> when (expectedState) {
            is ExpectedState.Connected -> orchestrator.connectIfNot(expectedState.device)
            ...
        }
        ...
    }
}.collect { }
```

`combine`'s transform is `suspend`. While the transform is suspended in
`orchestrator.connectIfNot(...)` or `disconnectCurrent()`, the upstream
flows continue to emit, and `combine`'s default behaviour is to *conflate*
intermediate values, delivering only the latest pair when the transform
becomes ready again. This means rapid state transitions (e.g.
Connecting → Connected → Disconnecting → Disconnected within the time of
one `connectIfNot` call) collapse to a single observed pair, and the
intermediate "Connected" state may be skipped entirely from the loop's
point of view.

That alone would be fine if the loop's only job were idempotent
reconciliation. But the loop also commits side effects directly:

- If we observe `Connected` (briefly) and `ExpectedState.Connected` with the
  same device, no action is taken. Good.
- If the loop is suspended in `connectIfNot(deviceA)` while the user picks
  `deviceB` in `getExpectedState()`, the next observation is
  `(Connected(deviceA), Connected(deviceB))` and the loop calls
  `connectIfNot(deviceB)` correctly.
- **But** if the loop is suspended in `disconnectCurrent()` (because the
  user just triggered "log out" → expected becomes Disconnected) and
  during that time the user re-selects `deviceA`, the next observation is
  `(Disconnected, Connected(deviceA))` and the loop calls
  `connectIfNot(deviceA)`. So far so good.

The actual race: when `forceRefreshConnection` cancels the current loop
mid-`connectIfNot`, the cancel doesn't interrupt the orchestrator's
internal `Mutex.withLock("connect")`. The new loop calls
`disconnectCurrent`, which in `disconnectInternalUnsafe` waits for
`currentDeviceLocal.disconnect()`. Two coroutines now contend for the
orchestrator's mutex. Which wins is timing-dependent — may produce
"connect" winning and the user's intended forced disconnect never
happens, or vice versa.

## Repro

1. User initiates forget/forceRefresh while a connection is being
   established.
2. The previous loop is mid-`connectIfNot(deviceA)`, parked inside the
   orchestrator's `connect` mutex.
3. `forceRefreshConnection` launches a new loop (CANCEL_PREVIOUS) which
   immediately calls `disconnectCurrent` — also parking on the orchestrator
   mutex.
4. The previous loop resumes (cancellation wins because `Mutex.withLock`
   resumes cancellation), bails out. The orchestrator finishes its connect
   path internally (since `connectIfNot` already pushed the
   `transportListenerFlow.emit(localTransportListener)` before checking
   cancellation? — depends on whether the suspension was past that point).
5. The new loop's `disconnectCurrent` resumes against whatever the
   orchestrator's mutable `currentDevice` field has, which may be the
   newly-set `deviceA` holder or null depending on race.

## Root Cause

`combine` + suspending transform + multiple side-effect-issuing branches is
a fragile pattern. Side effects should be issued from a downstream `.onEach`
or `collectLatest` block where cancellation semantics are explicit, and
intermediate state coalescing is intentional rather than incidental.

## Impact

- Under load (rapid user actions, flaky network), the connection loop's
  intent and the orchestrator's actual state can diverge.
- Bugs of this category surface as "device shows connecting forever" or
  "had to swipe-quit to recover" — exactly the kind of report that adding
  workaround `delay`s tends to mask without fixing.

## Suggested Fix

1. Switch the body to `collectLatest { (expected, real) -> ... }` so
   intermediate states can cancel an in-flight `connectIfNot` /
   `disconnectCurrent`.
2. Do not issue more than one side effect per emission; pick the action
   then `delay(small)` + re-read the latest state before issuing, OR
   model the loop as a state machine fed by both flows, with
   `coroutineContext.ensureActive()` checkpoints.
