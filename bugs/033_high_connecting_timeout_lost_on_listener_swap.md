# 10s "Connecting → Offline" timeout is silently dropped when transport listener swaps

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:43-64`

## Summary
The state pipeline is built as

```
transportListenerFlow.flatMapLatest { it.getState() ?: flowOf(DEFAULT_STATUS) }
                     .transformLatest { status ->
                         emit(status)
                         if (status is Connecting.InProgress) {
                             delay(CONNECTING_TIMEOUT)
                             emit(Connecting.Offline(...))
                         }
                     }
                     .stateIn(globalScope, Lazily, DEFAULT_STATUS)
```

`flatMapLatest` cancels the old downstream (and therefore the `transformLatest`) when a new value
arrives upstream. So whenever a new `FTransportListenerImpl` is emitted to `transportListenerFlow`,
the in-flight 10-second `delay` is cancelled. If the user calls `connectIfNot(deviceA)` and then
quickly `connectIfNot(deviceB)`, the first device's "Offline" transition never fires – which is
correct (deviceA is no longer relevant). But there's a more subtle case where the user calls
`connectIfNot(deviceA)` *twice in a row* and the `Connecting.InProgress` for the **same** device
is silently restarted from scratch each time, *without* emitting an Offline if the underlying
transport gets stuck.

Worse: after `connectIfNot(deviceB)` is called, the new listener's initial state is
`DEFAULT_STATUS = Disconnected(NOT_INITIALIZED)`. If the underlying transport for B is slow to
emit *any* status, the public state goes:

`Connecting.InProgress(deviceA)` → `Disconnected(NOT_INITIALIZED, device=null)` → eventually
`Connecting.InProgress(deviceB)`.

So observers see a transient `Disconnected` with `device=null` and `reason=NOT_INITIALIZED`
between two valid connecting states – a wholly fictitious state, not corresponding to any real
event in the device lifecycle.

## Reproduction / scenario
1. Subscribe to `getState()`.
2. Call `connectIfNot(deviceA)`. Public state goes `Disconnected(NOT_INITIALIZED) →
   Connecting.InProgress(deviceA)`.
3. While still `Connecting.InProgress`, call `connectIfNot(deviceB)`. The orchestrator emits a new
   `FTransportListenerImpl` to `transportListenerFlow`. Initially the new listener's state is
   `DEFAULT_STATUS`.
4. The downstream `flatMapLatest` re-subscribes to the new listener's state. The new listener has
   not received any `Connecting` from the new transport yet, so the public state goes back to
   `Disconnected(device=null, reason=NOT_INITIALIZED)`.
5. Eventually the new transport emits `Connecting`, and public state moves to
   `Connecting.InProgress(deviceB)`.

## Why it happens
- `transportListenerFlow` is a `MutableStateFlow<FTransportListenerImpl?>`; its initial value is
  `null`, mapped to `DEFAULT_STATUS`.
- New listeners are emitted via `transportListenerFlow.emit(localTransportListener)` *before* the
  underlying transport's `connect()` has reported any status – there is a real gap during which
  the new listener is in `DEFAULT_STATUS`.
- `flatMapLatest` faithfully re-subscribes and surfaces this transient default.
- The 10-second timeout in `transformLatest` is also cancelled by the swap, but that is mostly
  correct.

## Impact
- Observers see a spurious `Disconnected(device=null, NOT_INITIALIZED)` between two valid
  Connecting states. UI may flash "not connected" briefly. Worse, an observer that uses
  `filterIsInstance<Disconnected>()` to trigger a toast / log / analytics event fires falsely.
- The 10-second timer never fires for the abandoned listener – which is fine for the abandoned
  one, but means there is no way to express "I was connecting for >10s" if the user quickly
  retries.

## Suggested fix
- Initialize the new `FTransportListenerImpl`'s state to a synthetic `Connecting.InProgress`
  immediately on construction (the orchestrator knows the device, transport types, and that we
  intend to connect), so the new listener starts in `Connecting`, not `Disconnected`.
- Or add a `distinctUntilChanged` plus a filter to drop the transient default state during
  listener swaps.
