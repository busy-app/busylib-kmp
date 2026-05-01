# A connection that succeeds *just after* the 10s timeout still emits `Connected`, contradicting the previously emitted `Offline`

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:47-64`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FTransportListenerImpl.kt:40-72`

## Summary
The 10-second `CONNECTING_TIMEOUT` only adds a synthetic `Connecting.Offline` status into the
*outer* state flow; it does **not** cancel the in-flight `FDeviceHolder` connection. If the
underlying transport eventually succeeds at T = 10.5s and emits `Connected`, the public state
flow goes `Offline(...) → Connected(...)` – a transition the rest of the code does not expect.

Observers using `getState()` to drive UI/analytics may already have moved on (e.g., shown an
"Unable to connect, try again" screen, started a retry, or notified upstream services that the
device is offline). A surprise `Connected` event arriving 200ms later puts the system in a
dissonant state.

## Reproduction / scenario
1. `connectIfNot(deviceA)`. Public state: `Connecting.InProgress(deviceA)`.
2. Underlying transport is slow (say, BLE scan stuck); 10 seconds elapse.
3. Public state transitions to `Connecting.Offline(deviceA)`.
4. UI shows "device offline".
5. At T = 10.4s, transport finally connects and emits `Connected(api)`.
6. `FTransportListenerImpl.onStatusUpdate` updates state to `Connected(api)`.
7. Public state flow now contains `Connected(api)` – overriding the `Offline` UI state.

## Why it happens
- The timeout path in `stateFlow.transformLatest` only emits a *new value*; it does not cancel
  anything in the underlying `FDeviceHolder`.
- Equivalent: the design treats `Connecting.Offline` as a UI hint, not as a final state. But once
  emitted, it is immediately overridden by any subsequent transport event.

## Impact
- UI flicker: "Connecting → Offline → Connected" within seconds of each other.
- A user who has already pressed "Cancel" / "Retry" after seeing Offline gets a surprise
  Connected event for a device they no longer want.
- Cloud / LAN side effects (e.g., starting RPC, registering the device) trigger after the user
  had been told the device is unavailable.

## Suggested fix
- On transition to `Connecting.Offline`, also cancel the underlying `FDeviceHolder` (forcing it
  back to `Disconnected`) – that is, treat the timeout as authoritative.
- Or filter subsequent emissions in `transformLatest` so that once `Offline` is emitted, only
  another *new* listener (i.e., user explicitly re-trying) can take the state out of Offline.
