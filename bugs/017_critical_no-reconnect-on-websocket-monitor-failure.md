# critical — Cloud / non-Apple LAN monitor never recovers if WebSocket flow terminates

## Severity
critical

## Type
lack-of-feature

## Files
- `components/bridge/transport/tcp/common/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/common/monitor/WSEventsDeviceMonitor.kt:32-54`
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/streaming/FLanStreamingApiImpl.kt:36-57`
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/metainfo/FCloudStreamingApi.kt`

## Intended behavior (per project owner)
When the WebSocket event flow completes (transient blip), the LAN/Cloud monitor must auto-retry. The monitor is the layer that should drive recovery; an outer layer is not expected to do this.

## Summary
`WSEventsDeviceMonitor.startMonitoring()` relies entirely on the `eventSource.getEvents()` flow to drive transitions between `Connected` and `Connecting`. If the underlying WebSocket flow ends (because the session was closed or `wrapWebsocket`'s internal logic gave up), `transformLatest`/`collect` simply completes, the `singleJobScope.launch` coroutine ends, **and the monitor never tries again**. There is no `exponentialRetry`, no `repeat`, no `flow { while(true) }` wrapper. The listener is left in whatever the last status was (often `Connecting` because of the inactivity timeout), and the device is silently dead.

This affects:
- `CloudDeviceConnectionApiImpl` — only path on cloud transport.
- `FLanApiImpl` on Android & desktop JVM — only path on those targets (Apple uses `FAppleLanConnectionMonitor` which DOES restart, with its own bug).

The streaming flow is `shareIn(scope, WhileSubscribed(5s), 0)` — when the websocket fails the upstream `wrapWebsocket` block presumably emits an error which `shareIn` drops; on next subscription it tries again, but `WSEventsDeviceMonitor` is already a long-lived subscriber that hits `onCompletion` and exits.

## Repro
1. Bring up a LAN connection on Android or desktop.
2. Restart the device firmware (closes the WebSocket cleanly).
3. The shared upstream flow completes; `transformLatest { ... }.distinctUntilChanged().collect(listener::onStatusUpdate)` returns; the launched coroutine ends.
4. Bring the device back. The library never reconnects. User must explicitly call `disconnect()` + `connect(...)` again.

## Root Cause
The monitor treats the WebSocket flow as the source of truth and assumes the flow is infinite. There is no retry around the WebSocket session establishment, and the higher-level monitor doesn't loop. Project rule says use `exponentialRetry { ... }` from `core:ktx`.

## Impact
- "Permanent silent disconnect" — exactly the failure pattern AGENTS.md tags as **critical** ("no reconnect ever").
- User-visible bug: device card stays in `Connecting` indefinitely after a transient network blip on cloud.

## Suggested Fix
Wrap the body of `startMonitoring` in `exponentialRetry { ... }` and also re-collect after stream completion:

```kotlin
override suspend fun startMonitoring() {
    singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
        exponentialRetry(maxAttempts = Int.MAX_VALUE, initial = 1.seconds, max = 30.seconds) {
            listener.onStatusUpdate(Connecting(config.getTransportTypes()))
            eventSource.getEvents()
                .transformLatest { /* ... */ }
                .distinctUntilChanged()
                .collect(listener::onStatusUpdate)
            // if collect returns normally, treat as transient failure and retry
            error("event source completed")
        }
    }
}
```

For `FLanStreamingApiImpl`, also wrap `getWebSocket()` so the websocket itself reconnects on close.
