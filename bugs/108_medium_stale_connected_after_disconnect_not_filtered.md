# `FTransportListenerImpl.onStatusUpdate` does not guard against stale Connected after Disconnected

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FTransportListenerImpl.kt:40-72`

## Summary
The `Disconnected` branch (line 57-65) carefully avoids overwriting an existing `Disconnected`
state. But the symmetric guard does *not* exist for `Connected`/`Connecting`: a stale `Connected`
arriving from the transport after a `Disconnected` has been latched will cheerfully overwrite the
state, sending the system from `Disconnected → Connected` without going through any
`Connecting`/`Connecting` again. Combined with the `disconnect()` semantics in `FDeviceHolder` (it
*can* fail to fully cancel the underlying transport scope on the cancellation-throws path), a
brief window exists where the transport finishes its in-flight connect and emits Connected after
the orchestrator believed the device was already disconnected.

## Reproduction / scenario
1. Public state is `Connecting.InProgress(deviceA)`.
2. Caller invokes `disconnectCurrent()`. Orchestrator goes through cleanup, but the transport is
   slow to react.
3. Transport finishes its in-flight connect → `onStatusUpdate(Connected(api))` lands on the (now
   defunct) listener.
4. The listener's MutableStateFlow updates to `Connected(api)`.
5. *If* the subscription flow has not yet swapped to a new listener, the public state goes
   `Connecting.InProgress → Connected(api)`. Subscribers see a Connected event for a device they
   thought was already disconnected.

## Why it happens
- Asymmetric guard logic. The author hardened the Disconnected-after-Disconnected case (likely
  in response to the duplicate-Disconnected emission bug noted elsewhere), but did not consider
  Connected-after-Disconnected.

## Impact
- Brief but observable "ghost connect" event after disconnects, especially on slow transports.
- Cloud / RPC subsystems may attach to a now-defunct deviceApi instance.

## Suggested fix
Add a symmetric short-circuit: if `currentStatus is FDeviceConnectStatus.Disconnected` and the
incoming status is *also* a "post-disconnect" event for the same device, ignore. Or, more
robustly, mark the `FTransportListenerImpl` as "closed" after the holder it represents is torn
down, and drop subsequent `onStatusUpdate` calls on a closed listener.
