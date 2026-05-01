# `FDeviceHolder` emits duplicate Disconnected callbacks (transport listener + invokeOnCompletion)

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:101-119`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceHolder.kt:90-99`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:92-101`

## Summary
On disconnect (whether requested or via the underlying transport), the orchestrator's listener is
called *twice* with `Disconnected`:

1. The underlying transport reports `Disconnected` via `transportConnectionListener` (forwarded to
   `listener.invoke(this, Disconnected)`).
2. Then `disconnect()` cancels `scope.coroutineContext.job`, which fires the `invokeOnCompletion`
   handler from the `init` block – which *also* invokes `listener.invoke(this, Disconnected)`.

Each `Disconnected` callback in the orchestrator goes through `onInternalDisconnect`, which queues
a `globalScope.launch { withLock(mutex, "disconnect_internal") { ... } }`. Two redundant launches
are queued; both run sequentially, and both call `postAction()` (i.e.
`localTransportListener.onStatusUpdate(config, Disconnected)`), which writes the same status
twice. More importantly, the *second* `withLock` runs after the first has already nulled
`currentDevice` to the now-old holder; the old holder is no longer current, so disconnection is
skipped, but `postAction` still fires.

## Reproduction / scenario
1. Transport reports `Disconnected` (e.g., BLE link dropped). Listener fires – orchestrator queues
   one `globalScope.launch { ... }` to clean up.
2. The transport may *or may not* also cancel the holder's scope as part of its own cleanup. If
   the transport cancels the scope, the `init { invokeOnCompletion { ... } }` callback also
   invokes the listener with `Disconnected`. A *second* `globalScope.launch { ... }` is queued.
3. Both launches eventually run; the orchestrator calls `disconnectInternalUnsafe(deviceHolder)`
   twice. The first does the real cleanup; the second is a no-op for state but still calls
   `postAction()`, which sets the flow to `Disconnected` (already disconnected).

The same duplication happens on the `disconnect()` path:

1. Caller invokes `disconnectCurrent()` → `disconnectInternalUnsafe()` →
   `currentDeviceLocal.disconnect()`.
2. Inside `disconnect()`, `scope.coroutineContext.job.cancelAndJoin()` cancels the scope →
   `invokeOnCompletion` fires → listener called with `Disconnected`.
3. Listener path (line 92-101 in orchestrator) sees `Disconnected` → calls
   `onInternalDisconnect(deviceHolder, postAction)` → queues `globalScope.launch { withLock(...) }`
   while we are *still inside* `withLock("disconnect")`.
4. The queued launch waits for the mutex; we release; it acquires; finds `currentDevice == null`
   (we already nulled it) so the referential-equality check `currentDeviceLocal !== configToDisconnect`
   evaluates `null !== oldHolder` → true → "Tried to disconnect not current device, skip" – good.
5. But `postAction()` still calls `localTransportListener.onStatusUpdate(config, Disconnected)`,
   producing yet another `Disconnected` emission to the (now disconnected) listener.

## Why it happens
- The design has two independent paths that can produce a `Disconnected` event for the same
  underlying disconnect, with no de-duplication.
- `invokeOnCompletion` is invoked even on the deliberate-disconnect path (because the disconnect
  flow itself cancels the scope).
- `onStatusUpdate(Disconnected)` does protect against state-flow churn (it preserves the existing
  `Disconnected` value) – good. But it does not guard against the duplicate `globalScope.launch`
  pile-up.

## Impact
- Spurious `globalScope.launch` work, including a redundant `withLock` cycle, every disconnect.
- Log noise that obscures genuine error paths ("Received status update for device X: Disconnected"
  appears twice every disconnect).
- The pile-up of queued launches becomes a real ordering hazard when combined with a fast
  reconnect: a new `connectIfNot` may run between the two launches, and the *second* launch then
  observes `currentDevice` as the new holder and *does* call `postAction()` – emitting a stale
  `Disconnected` for the *new* listener if the closure references were re-bound (they are not, in
  current code, but a small refactor could break this fragile arrangement).
- A higher-level consumer (e.g. RPC `flowOf(state).filterIsInstance<Disconnected>()`) sees two
  Disconnected events for one logical disconnect; if the consumer launches a side effect per
  Disconnected event (analytics, retry counter, "show toast") it fires twice.

## Suggested fix
- De-dup at the holder level: track a `disconnected: AtomicBoolean` and emit `Disconnected` to the
  listener at most once.
- Or remove the `invokeOnCompletion` listener emission entirely if the underlying transport
  guarantees reporting Disconnected.
- Defensive: guard `onInternalDisconnect` so the launched cleanup is a no-op if already cleaned up
  for that holder.
