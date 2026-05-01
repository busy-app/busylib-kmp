# `transportListenerFlow.emit(...)` inside the orchestrator mutex can introduce blocking on slow downstream collectors

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:87`

## Summary
`transportListenerFlow` is a `MutableStateFlow`, whose `emit` is documented as never suspending
when there are no slow collectors. However, `MutableStateFlow.emit` can suspend if the value is
the same as the current value (it returns immediately) or if there are downstream conflated
collectors that haven't yet processed the previous value. In practice, MSF.emit on a different
value is non-blocking. So no real bug here in isolation.

But `stateFlow` further down is built with `.stateIn(globalScope, SharingStarted.Lazily,
DEFAULT_STATUS)`. The `transformLatest` chain has a `delay(CONNECTING_TIMEOUT)` (10s) inside it.
While `stateIn` is *Lazily*-shared, suspensions inside upstream operators only affect the
upstream coroutine on `globalScope`, not `transportListenerFlow.emit`.

The actual subtle issue: between `transportListenerFlow.emit(localTransportListener)` (line 87)
and `currentDevice = deviceHolderFactory.build(...)` (line 89), the state pipeline observes a new
listener whose state is `DEFAULT_STATUS`. The orchestrator publishes "Disconnected
(NOT_INITIALIZED)" *with the mutex still held*. If a downstream consumer is doing `getState()
.first { it is Connected }` and triggers a reentrant call back into the orchestrator (e.g., they
synchronously call `connectIfNot` from the collector), the consumer's `connectIfNot` will deadlock
on the mutex because the same coroutine cannot acquire the mutex twice (kotlinx Mutex is
non-reentrant).

If consumers always `launch` from collectors, this is fine. But "always launch" is not enforced
anywhere – any consumer that calls a suspend function from `getState().collect { ... }` and
inside that collect ends up calling back into the orchestrator on the *same* coroutine will
deadlock.

## Reproduction / scenario
1. A higher-level service calls `getState().collect { state -> if (state is Disconnected) {
   orchestrator.connectIfNot(rememberedConfig) } }`.
2. The collect is on the same coroutine as the orchestrator's mutex critical section (e.g., the
   service called `connectIfNot` first to seed the flow).
3. Inside the orchestrator's `connectIfNot`, it emits a new listener; `flatMapLatest`
   re-subscribes; the collector fires synchronously with `Disconnected(NOT_INITIALIZED)`.
4. The collector calls `connectIfNot(rememberedConfig)` synchronously.
5. Mutex is non-reentrant → deadlock.

## Why it happens
- `transportListenerFlow.emit` happens inside the mutex.
- Consumers might (incorrectly) try to call back into the orchestrator from a collector.
- Mutex is non-reentrant in kotlinx-coroutines.

## Impact
- Latent deadlock if a consumer ever does the wrong thing. Hard to debug.

## Suggested fix
- Move all flow-emit work outside the mutex critical section. Compute new state inside the lock,
  emit after release.
- Or document loudly that consumers must `launch` from collectors and not call back.
- Or use `MutableStateFlow.value = ...` (non-suspend) which is documented as wait-free.
