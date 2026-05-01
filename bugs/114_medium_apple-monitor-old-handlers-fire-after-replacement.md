# medium — Apple monitor: old `nw_connection` handlers may still fire after replacement

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/appleMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/monitor/FAppleLanConnectionMonitor.kt:73-94`

## Summary
`createConnection()` does:
```kotlin
connection?.let { nw_connection_cancel(it) }
val createdConnection = nw_connection_create(...)
…
connection = createdConnection
```

It cancels the previous connection but **does NOT clear its handlers** (state-changed, viability-changed, path-changed). After cancellation, the OS will still deliver one final `nw_connection_state_cancelled` event for the old connection. That callback is still wired to the same monitor instance, so it executes `runBlocking { handleStateUpdate(cancelled) }` → `restartMonitoring()`.

While restart is happening (or has happened) on a NEW connection, this stale callback from the OLD connection causes a redundant `restartMonitoring()` which may:
- be SKIPPED by `SKIP_IF_RUNNING` (silent drop), OR
- run after the new connection became ready, tearing it down again.

The public `stopMonitoring()` correctly clears handlers (`nw_connection_set_state_changed_handler(localConnection, null)`), but `createConnection()` does not.

## Repro
1. Trigger a fast restart cycle (server flap).
2. Observe in logs: `#handleStateUpdate Connection cancelled` arriving AFTER a new connection has reached `ready`.

## Root Cause
Asymmetric teardown: `stopMonitoring` clears handlers, `createConnection`'s precondition does not.

## Impact
- Sporadic spurious restarts.
- Log noise; race-condition flakiness in tests.

## Suggested Fix
```kotlin
connection?.let { old ->
    nw_connection_set_state_changed_handler(old, null)
    nw_connection_set_viability_changed_handler(old, null)
    nw_connection_set_path_changed_handler(old, null)
    nw_connection_cancel(old)
}
```

(Same teardown as `stopMonitoring` — extract to a private `tearDownConnection(...)` helper.)
