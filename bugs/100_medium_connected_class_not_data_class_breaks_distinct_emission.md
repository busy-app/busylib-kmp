# `FDeviceConnectStatus.Connected` is `class` not `data class`; breaks distinct-emission semantics

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/api/src/commonMain/kotlin/net/flipper/bridge/connection/orchestrator/api/model/FDeviceConnectStatus.kt:43-48`

## Summary
`FDeviceConnectStatus.Connected` is declared as a plain `class`, while every other variant in the
same `sealed interface` is a `data class`. As a result, two `Connected` values with identical
fields are *not* equal (default `equals` is reference equality). Several places use
`MutableStateFlow.update`/`updateAndGet` (`FTransportListenerImpl.kt:41`) and similar
`compareAndSet`-based primitives that rely on `equals` for de-duplication. Equal-by-fields
`Connected` values will be treated as different, causing duplicate emissions.

In addition, downstream collectors that use `distinctUntilChanged()` will see every
`Connected(...)` as a fresh emission, even if the underlying device hasn't changed.

## Reproduction / scenario
1. `FTransportListenerImpl.onStatusUpdate(Connected(api1))` – state updates to Connected.
2. Same status arrives a second time (e.g., transport re-emits a stable Connected after a
   negligible heartbeat blip): `state.updateAndGet` constructs a new `Connected(api1', ...)`. Even
   if `api1` is the same instance, the wrapping `Connected` is a fresh object – not equal to the
   previous Connected.
3. Subscribers see two emissions.
4. Worse: subscribers using `state.value` to compare across times will always see "different"
   even when nothing changed.

## Why it happens
- Author probably could not (or didn't) declare `data class Connected` because `CoroutineScope`
  and `FConnectedDeviceApi` don't have meaningful `equals`. But the consequence is that *every*
  emission of `Connected` is "different" by reference.

## Impact
- Duplicate downstream work (analytics events, RPC connection setup, UI re-renders).
- `distinctUntilChanged()` is silently broken for the most common state.

## Suggested fix
Either:
1. Make `Connected` a `data class` and document that `equals` is by `(device, transportType,
   System.identityHashCode(deviceApi))` – or override equals/hashCode explicitly on the relevant
   subset of fields (e.g., `device.uniqueId` + `transportType`).
2. Or have `FTransportListenerImpl.onStatusUpdate` skip writing `Connected` if the previous state
   is already `Connected` with the same `deviceApi` reference (mirroring the
   `Disconnected → Disconnected` short-circuit).
