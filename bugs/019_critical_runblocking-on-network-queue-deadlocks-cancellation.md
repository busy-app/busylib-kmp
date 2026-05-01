# critical — `runBlocking` inside `nw_connection` state-changed handler can deadlock cancellation

## Severity
critical

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/appleMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/monitor/FAppleLanConnectionMonitor.kt:155-166` (callback)
- Same file `:65-71` (`restartMonitoring`), `:208-220` (`stopMonitoring`)

## Summary
`collectConnectionEvents` registers a state-changed handler that calls `runBlocking { handleStateUpdate(...) }`. The handler runs on the serial `dispatch_queue_create("net.flipper.lan.connection", ...)`. `handleStateUpdate` may call `restartMonitoring()` which `launch`es a coroutine that calls `stopMonitoring()` — and `stopMonitoring()` calls `nw_connection_force_cancel`. `nw_connection_force_cancel` schedules a cancelled-state callback ON THE SAME serial queue. That callback can only run after the current handler returns, but the current handler is still inside `runBlocking` waiting for the launched coroutine, which itself is waiting for `force_cancel` to take effect. Even worse, AGENTS.md forbids `runBlocking` in coroutine code.

The accompanying test (`FAppleLanConnectionMonitorTest.GIVEN_connected_WHEN_server_restarts_THEN_reconnects_successfully`) explicitly documents this deadlock scenario in a comment but only verifies "doesn't always deadlock", not "never deadlocks" — i.e. the comment acknowledges the bug remains.

## Repro
1. Connect successfully (state `ready`, callback fires `runBlocking { handleStateUpdate(ready, null) }` — completes quickly because no restart).
2. Server kills connection; kernel delivers `state_failed` on the serial dispatch queue.
3. Handler enters `runBlocking { handleStateUpdate(failed, ...) }` → `restartMonitoring()` → `restartMonitoringScope.launch(SKIP_IF_RUNNING) { stopMonitoring(); startMonitoring() }`.
4. `restartMonitoring` returns immediately (fire-and-forget) — but `runBlocking` then completes for the failed event. So far OK.
5. The launched coroutine calls `stopMonitoring()` → `nw_connection_force_cancel` → kernel queues `state_cancelled` callback onto the serial queue.
6. The dispatch queue then delivers `state_cancelled`, callback enters `runBlocking { handleStateUpdate(cancelled) }` → `restartMonitoring()` again → `SKIP_IF_RUNNING` may skip OR may run — both paths block the queue while another `force_cancel` is requested mid-create.
7. If the inner state handler is fired while the outer `runBlocking` for a previous handler is still going (callbacks delivered serially but the `runBlocking` from event N could await something that requires event N+1 for resolution if any future signal needs the queue), the queue is wedged.

In addition, **`runBlocking` blocks the kernel network queue for the entire duration of every `handleStateUpdate`** (which itself emits to a `MutableSharedFlow` etc.). This stalls all subsequent state/viability/path callbacks, masking real network events and making timing tests racy.

## Root Cause
The state-changed handler from Network.framework is not `suspend`. The author bridged the two worlds with `runBlocking`, which is the wrong tool. The correct pattern is to dispatch the event to a coroutine via a `Channel` / `MutableSharedFlow.tryEmit` and process it on a dedicated coroutine collector.

## Impact
- Deadlock potential during reconnect cycles on iOS / macOS — the only LAN-monitor implementation on Apple targets.
- `restartMonitoringScope` uses `SKIP_IF_RUNNING`, so legitimate later events may be silently dropped while the first restart is in progress (test suite confirms this — see `GIVEN_connected_WHEN_server_closes_THEN_no_duplicate_connecting_burst` enforces "exactly 1 Connecting" which means events 2..N are deliberately swallowed).
- The serial network queue is blocked for every state transition; on a flaky link the queue backs up.

## Suggested Fix
Replace `runBlocking` with a non-blocking emit:

```kotlin
private val events = Channel<StateEvent>(Channel.UNLIMITED)

init {
    scope.launch { for (e in events) handleStateUpdate(e.state, e.error) }
}

private fun collectConnectionEvents(connection: nw_connection_t) {
    nw_connection_set_state_changed_handler(connection) { state, error ->
        events.trySend(StateEvent(state, error?.asKotlinNwError()))
    }
}
```

Cancel the consumer in `stopMonitoring()`. Also reconsider `SKIP_IF_RUNNING` — use `CANCEL_PREVIOUS` or a dedicated `MutableStateFlow<DesiredState>` driver so state changes are never silently dropped.
