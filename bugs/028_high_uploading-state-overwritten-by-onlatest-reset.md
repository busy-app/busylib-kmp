# `Uploading` state is briefly reset to `Pending` when device feature flips, racing with progress emissions

## Type
broken-feature

**Severity:** high

**Files:**
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/uploader/api/FirmwareUploaderApiImpl.kt` (lines 38–85)

## Summary

`FirmwareUploaderApiImpl.uploadAndInstall` uses `onLatest { ... }` over the `FRpcFeatureApi` flow inside
`uploadAndInstall` so that any change in the underlying RPC feature (e.g. transient feature re-publication)
restarts the upload. The block's `onEach { _state.emit(FirmwareUploaderState.Pending) }` then
`onLatest { _state.emit(Uploading(0, 0)) ... }` produces this sequence on every emission:

```
Uploading(...) -> Pending -> Uploading(0, 0) -> Uploading(0, size) -> Uploading(bytesUploaded, size)
```

That `Pending` flash makes consumer UIs show "no upload in progress" mid-transfer. It also resets accumulated
progress to zero. If the upstream `fFeatureProvider.get<FRpcFeatureApi>()` flow flickers (and `FRpcFeatureApi`
is a `StateFlow`-backed value, which republishes on subscription), the user sees the upload restart visually
even though the underlying `postUpdate` is still in flight against the previous `fFeatureApi`.

A more serious second-order effect: if the feature actually changes (transport switch BLE↔LAN), `onLatest`
cancels the in-flight `postUpdate`. Because the cancellation interrupts `bytesFlow` mid-write, the device may
be left with a half-written firmware blob it cannot recover from until reboot.

## Repro

1. Start a LAN upload.
2. Have any code re-emit on `FRpcFeatureApi`'s status flow (e.g. user toggles a permission, transport-switch
   logic re-publishes the feature). On real devices this happens often during connection churn.
3. Observe `FwUpdateState` flips Uploading → Pending → Uploading(0, …) and the displayed progress resets to
   zero.
4. If the upstream value is *different* (transport switch), the in-flight upload is cancelled mid-stream.

## Root cause

```kotlin
fFeatureProvider.get<FRpcFeatureApi>()
    ...
    .onEach { _state.emit(FirmwareUploaderState.Pending) }
    .onLatest { fFeatureApi ->
        _state.emit(FirmwareUploaderState.Uploading(0, 0))
        ...
    }
    .catch { ... }
    .first()
```

`onEach` runs for every emission, including the very first one, and emits `Pending` to the state machine
before the first byte is even read. There is also no de-duplication (`distinctUntilChanged`) of the upstream
feature value, so identical re-emissions still trigger the reset.

## Impact

- User-visible progress regression in the middle of an upload.
- Real-world transport switch between BLE and LAN during an upload silently aborts the upload without
  surfacing failure.

## Suggested fix

- `distinctUntilChangedBy { it.featureApi }` upstream so identical emissions don't reset state.
- Drop the `onEach { Pending }` step; `onLatest` already provides the cancellation semantics.
- On feature-cancel, emit a real `Failed` (not `Pending`) so the orchestrator can react and the user sees
  what happened.
