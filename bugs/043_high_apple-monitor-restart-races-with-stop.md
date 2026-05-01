# high — `restartMonitoring` race: pending restart still emits `Connecting` after `stopMonitoring`

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/appleMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/monitor/FAppleLanConnectionMonitor.kt:65-71` (`restartMonitoring`)
- Same file `:208-220` (`stopMonitoring`)

## Summary
`restartMonitoring()` does:

```kotlin
restartMonitoringScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
    listener.onStatusUpdate(Connecting(...))
    stopMonitoring()
    startMonitoring()
}
```

`stopMonitoring()` (the public override) only cancels the `nw_connection_t` — it does NOT cancel `restartMonitoringScope`. After a user-initiated `stopMonitoring()`, a still-queued or just-launched restart coroutine will:
1. Emit `Connecting(...)` to the listener (spurious — the user explicitly stopped).
2. Call internal `stopMonitoring()` (idempotent OK).
3. Call `startMonitoring()` — which **creates a new `nw_connection`, sets handlers, and starts it again**, completely re-establishing the connection the user just torn down.

The companion test `GIVEN_connected_WHEN_stopMonitoring_THEN_no_new_statuses` documents this exact concern but only checks for absence of statuses for 2 seconds — race-prone and may pass in CI while real users see ghost reconnects.

## Repro
1. Connection is `Connected`.
2. The state callback fires `failed` (e.g. transient packet loss). `restartMonitoring()` schedules a restart coroutine.
3. Immediately after, application code calls `monitor.stopMonitoring()` (e.g. user navigated away).
4. The scheduled restart coroutine runs and re-creates the `nw_connection_t` — connection comes back up unwanted, fires `ready` → `Connected` to the listener.
5. The application thinks it disconnected; the device thinks it's still connected; HTTP requests now succeed when they shouldn't.

## Root Cause
`stopMonitoring()` is not symmetric with the restart scope. It cancels the network connection but not the restart job that's about to re-create one.

## Impact
- Spurious `Connected` after deliberate disconnect — listener and device-state desync.
- Holds the TCP connection alive past the user's intent — leaks battery / data, also risks state confusion in upper layers.
- Race-prone tests give false confidence.

## Suggested Fix
```kotlin
override fun stopMonitoring() {
    restartMonitoringScope.cancelPrevious()  // cancel pending restart
    connectionLock.withLock { /* ... existing code ... */ }
}
```

Also: the inner `stopMonitoring()` invoked from inside the launched restart coroutine should be a private helper that only does the connection-cancel part, not the public override that future-proofs cancellation.
