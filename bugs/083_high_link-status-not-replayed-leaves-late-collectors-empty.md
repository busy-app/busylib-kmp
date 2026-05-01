# `FLinkedInfoOnDemandFeatureApi.status` drops the latest value after collection ends

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/feature/link/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/link/check/onready/api/FLinkInfoOnReadyFeatureApiImpl.kt` (lines 46–47)
- `components/bridge/feature/link/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/link/check/ondemand/api/FLinkedInfoOnDemandFeatureApi.kt`

## Summary
`status: WrappedFlow<LinkedAccountInfo>` is built as `_status.filterNotNull().wrap()`. `_status` is a `MutableStateFlow<LinkedAccountInfo?>(null)`, but `filterNotNull()` strips its `StateFlow` semantics — every collector goes through a fresh `Flow` chain. That alone would be fine because `MutableStateFlow` always replays its current value, **except** that the value can still be `null` (the initial state) at the time a late collector subscribes. In that case the collector waits for the next `emit` and sees nothing while link state has actually been computed elsewhere. Worse: in `FFinishSetupFeatureApiImpl` this flow is `combine`d with others; a `null` initial state suppresses the entire setup view.

## Repro
1. Call `FFeatureProvider.getSync<FLinkedInfoOnDemandFeatureApi>()` after `onReady()` has finished and emitted to `_status`. Subscribe to `status`.
   - If `_status.value` is `null` (e.g. transient cancellation of the `tryCheckLinkedInfo` job because of `SingleJobMode.CANCEL_PREVIOUS` racing with `onReady`), the collector blocks indefinitely.
2. Even if `_status.value` is non-null, every new collector triggers re-collection of the StateFlow but `filterNotNull` does not preserve replay semantics for SKIE consumers using `WrappedFlow` (which does not implement StateFlow contract).

## Root Cause
- The internal cache is a nullable `StateFlow`, but the public surface is a non-null `WrappedFlow`. Any consumer expecting "always-current latest" semantics is broken because:
  - The wrapper isn't `WrappedStateFlow`.
  - The initial value is `null`, deliberately filtered out, so there is no replay until the first non-null emit.
- Combined with the cancellation race in `tryCheckLinkedInfo` (`SingleJobMode.CANCEL_PREVIOUS`), there are scenarios where `_status` never receives a value.

## Impact
- `FFinishSetupFeatureApiImpl.taskListResourceFlow` (which `combine`s `status`) emits nothing while we wait for the first link emission, leading to UI hangs on the setup screen.
- Late subscribers (after onReady) get no replay value; they have to wait for the next status change which may never come (steady state).
- Cross-platform consumers via SKIE assume `status` behaves like a StateFlow but it does not.

## Suggested Fix
- Expose `_status.filterNotNull().stateIn(scope, SharingStarted.Eagerly, defaultLinkedAccountInfo).wrap()` and have the model carry an explicit `Loading` variant (or a sealed `LinkedAccountInfo.Loading`) so the initial state is meaningful.
- Or expose `WrappedStateFlow<LinkedAccountInfo>` with a sentinel "Loading" value.
- Avoid `SingleJobMode.CANCEL_PREVIOUS` racing with `onReady` by guarding with a `Mutex` and re-running rather than cancelling, so we never lose the only chance to emit.
