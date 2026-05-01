# medium — `WSEventsDeviceMonitor` inactivity timeout downgrades `Connected` → `Connecting` even on healthy idle link

## Severity
medium

## Type
broken-feature

## Files
- `components/bridge/transport/tcp/common/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/common/monitor/WSEventsDeviceMonitor.kt:20,37-49`

## Summary
```kotlin
private val INACTIVITY_TIMEOUT = 10.seconds
…
val connectingState = wsEventFlow.transformLatest {
    emit(Connected(...))
    delay(INACTIVITY_TIMEOUT) // Should be interrupted by any event from websocket
    emit(Connecting(...))
}.distinctUntilChanged()
```

Logic: any inbound frame transitions to `Connected`, then if no other frame arrives within 10s the state degrades to `Connecting`. This conflates "no application-layer event" with "no transport-layer connectivity". An idle but healthy device that simply has nothing to report will be marked `Connecting` every 10 seconds — the listener will see oscillation:

```
event → Connected → (10s no event) → Connecting → event → Connected → (10s) → Connecting → ...
```

There is no underlying ping mechanism to verify connectivity (the WebSocket plugin in Ktor sends control PINGs but those are not surfaced as `Frame.Binary`, so they don't reset the timer).

`distinctUntilChanged()` does NOT deduplicate by event ordinal — `Connected(scope, deviceApi, ...)` data class equality includes `scope` (a `CoroutineScope` reference) so each emit is treated as a distinct value, blowing past the deduplication intent.

## Repro
1. Connect to a quiet device (no status events for >10s).
2. Listener receives `Connecting` despite device being fully reachable.

## Root Cause
- Liveness should be measured at transport layer (WS pings, TCP keepalive), not app layer.
- `Connected` data class equality with `scope` makes `distinctUntilChanged` useless.

## Impact
- UI flicker / misleading state for healthy idle connections.
- Application logic that gates retries on `Connecting` may misfire periodically.

## Suggested Fix
1. Tie liveness to actual transport heartbeat. If using Ktor websockets, configure `pingInterval` and observe `Frame.Pong` (or rely on Ktor's auto-close on missed pong).
2. If app-level liveness is desired, send an explicit "ping" frame on the device protocol every 5s and only degrade after several missed pings.
3. Override `equals/hashCode` for `FInternalTransportConnectionStatus.Connected` to ignore `scope`/`deviceApi`, OR call `distinctUntilChangedBy { it::class }` to deduplicate by status type only.
