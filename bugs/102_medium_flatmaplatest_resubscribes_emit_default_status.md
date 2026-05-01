# `flatMapLatest` over `transportListenerFlow` re-emits `DEFAULT_STATUS` on every listener swap

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:43-46`

## Summary
```kotlin
private val transportListenerFlow = MutableStateFlow<FTransportListenerImpl?>(null)
private val rawStateFlow = transportListenerFlow.flatMapLatest {
    it?.getState() ?: flowOf(FTransportListenerImpl.DEFAULT_STATUS)
}
```

When a new `FTransportListenerImpl` is emitted to `transportListenerFlow`, the previous
inner-flow subscription is cancelled and a new one is subscribed. The new
`FTransportListenerImpl.state` starts at `DEFAULT_STATUS = Disconnected(NOT_INITIALIZED)`. So the
public state flow always emits `Disconnected(NOT_INITIALIZED)` once between every successful
disconnect → reconnect cycle, *even when the new transport hasn't reported anything yet*.

## Reproduction / scenario
1. Public state: `Connected(api1)`.
2. `connectIfNot(deviceA)` (same device) called and `tryToUpdateConnectionConfig` fails.
3. `disconnectInternalUnsafe` runs.
4. New `FTransportListenerImpl` is created with state = `DEFAULT_STATUS`.
5. `transportListenerFlow.emit(newListener)`.
6. `flatMapLatest` re-subscribes to `newListener.getState()` → emits `Disconnected(NOT_INITIALIZED)`.
7. Public state now shows `Disconnected(NOT_INITIALIZED, device=null)`.
8. Underlying transport eventually reports `Connecting` → public state moves on.

The transient `Disconnected(NOT_INITIALIZED, device=null)` is misleading – `NOT_INITIALIZED`
implies "the orchestrator is fresh and has never connected", but the truth is "we are
mid-reconnect to deviceA".

## Why it happens
- `DEFAULT_STATUS` is shared between "orchestrator just started" and "new listener just created"
  – two semantically different states.

## Impact
- Misleading state visible to consumers.
- Combined with `device = null`, observers of the public state may see a "Forgot which device
  we're connected to" event during every reconnect.

## Suggested fix
- Initialize each new `FTransportListenerImpl` with a distinct "transitioning" state, not the
  literal `DEFAULT_STATUS` constant. For example, the orchestrator already knows the device and
  the intent at the moment of construction; pass them in:

```kotlin
class FTransportListenerImpl(
    config: BUSYBar,
    initialStatus: FDeviceConnectStatus = DEFAULT_STATUS
) {
    private val state = MutableStateFlow(initialStatus)
}
```

and call from the orchestrator with a `Connecting.InProgress(device, CONNECTING, transportTypes)`
seeded value.
