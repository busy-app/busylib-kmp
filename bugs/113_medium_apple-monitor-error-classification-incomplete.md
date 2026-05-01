# medium — Apple `KotlinNwError` classification triggers reconnect on too few cases

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/appleMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/monitor/FAppleLanConnectionMonitor.kt:96-153`
- `components/bridge/transport/tcp/lan/impl/src/appleMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/model/KotlinNwError.kt`

## Summary
`handleStateUpdate` only restarts on `TimedOut` / `ResetByPeer`:
```kotlin
when (error) {
    null,
    is KotlinNwError.HostIsDown,
    is KotlinNwError.Unknown,
    is KotlinNwError.NoRouteToHost -> Unit

    is KotlinNwError.TimedOut,
    is KotlinNwError.ResetByPeer -> {
        restartMonitoring()
        return
    }
}
```

For `HostIsDown` and `NoRouteToHost`, control falls through to the state-based switch — which does restart on `state_failed`, but a `nw_connection` may sit in `state_waiting` for a long time (Network.framework holds the connection in waiting until the OS believes routing exists). When `error = HostIsDown` and `state = waiting`, the code emits `Connecting` but never explicitly retries with a fresh connection. The OS retry policy is opaque and slow.

`KotlinNwError.Unknown` covers ALL other Network.framework errors — including TLS errors, DNS errors, EHOSTUNREACH variants, "policy denied" — and they're all silently ignored. Real users will see "stuck Connecting" and no logs of any anomaly because the `Unknown` branch only sets `Unit`.

Also the error code list is incomplete:
- ECONNREFUSED (61) — not classified.
- ENETDOWN (50) / ENETUNREACH (51) — not classified.
- ECONNABORTED (53) — not classified.

These all fall to `Unknown` → no restart.

## Repro
1. Point the monitor at a host that returns ECONNREFUSED (port closed but reachable).
2. Connection enters `state_failed` with `Unknown(61)`. Logged but no specific handling.
3. Restart happens via state-failed branch (good!), but with no backoff — see related bug `critical_no-reconnect-on-websocket-monitor-failure.md`.

## Root Cause
Hand-coded error taxonomy that's missing common cases.

## Impact
- Network errors fall into `Unknown`; debugging is harder; no telemetry separates classes.
- Some error/state combinations don't trigger restart, leaving monitor stuck.

## Suggested Fix
1. Treat any non-null error as "trigger restart" by default; whitelist only the cases we know are transient/benign.
2. Add ECONNREFUSED, ENETDOWN, ENETUNREACH to `KotlinNwError`.
3. Always log the raw `nw_error_get_error_code` so on-device diagnostics have something to work with.
