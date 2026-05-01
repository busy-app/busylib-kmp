# high — `SKIP_IF_RUNNING` causes Apple monitor to silently drop legitimate state changes

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/appleMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/monitor/FAppleLanConnectionMonitor.kt:65-71`

## Summary
`restartMonitoring()` uses `SingleJobMode.SKIP_IF_RUNNING`. If a restart is already in flight, ALL subsequent triggers — viability lost, path changed, second `failed`, reset-by-peer — are silently ignored. The unit test `GIVEN_connected_WHEN_server_closes_THEN_no_duplicate_connecting_burst` actively codifies this as desired ("expected exactly 1 Connecting"), but in production it means we cannot react to a second failure that happens during the restart window.

Concrete bad sequence:
1. Connection fails (`failed`); restart launched, currently inside `startMonitoring()` waiting for the new `nw_connection_t` to reach `ready`.
2. While waiting, the network path goes away entirely (e.g. Wi-Fi off). `path_changed` fires → `restartMonitoring()` → SKIPPED.
3. The original restart's new connection eventually transitions to `waiting` (host unreachable). `state_changed(waiting)` arrives → `restartMonitoring()` → also SKIPPED if the original restart is still active (it isn't, but the sequence is order-dependent).
4. Net effect: monitor parked in stale `Connecting` until something else jolts it.

## Root Cause
`SKIP_IF_RUNNING` was chosen to suppress the visible "burst of Connecting" caused by 3 handlers (state, viability, path) all firing within milliseconds of the same underlying event — but it conflates "concurrent duplicate trigger" with "subsequent independent trigger".

## Impact
- Monitor stuck in stale state under flapping network conditions.
- Test asserts a behavior that masks the bug: real consumers care about state convergence, not exact `Connecting` count.

## Suggested Fix
Replace the `SKIP_IF_RUNNING` "fan-in 3 handlers into 1 restart" with a `MutableStateFlow<DesiredState>` driver:

```kotlin
private val desiredState = MutableStateFlow<DesiredState>(Stopped)

init {
    scope.launch {
        desiredState.collectLatest { state ->
            when (state) {
                Started -> runConnectionCycle()  // sets up nw_connection, awaits ready, …
                Stopped -> { /* ensure connection cancelled */ }
            }
        }
    }
}

// handlers just call:
private fun signalRestart() {
    desiredState.value = Started   // collectLatest cancels previous cycle
}
```

`collectLatest` correctly cancels the in-flight cycle and starts a fresh one — all signals are honored, no events lost.
