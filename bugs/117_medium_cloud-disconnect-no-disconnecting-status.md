# medium — `disconnect()` skips `Disconnecting` status; no monitor cancellation on cloud

## Severity
medium

## Type
broken-feature

## Files
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FCloudApiImpl.kt:68-72`
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FLanApiImpl.kt:73-79`

## Summary
`FInternalTransportConnectionStatus` defines a `Disconnecting` state but neither `FCloudApiImpl.disconnect()` nor `FLanApiImpl.disconnect()` emit it before performing the (potentially long) teardown. Consumers that show "disconnecting…" UX during socket teardown will never see it.

Also: `FCloudApiImpl.disconnect()` does not cancel the monitor job started in `init`. The monitor coroutine (running in `scope`) keeps observing the websocket flow even after `disconnect()` and emits a final `Connecting` after the inactivity timeout, contradicting the `Disconnected` status that was just sent. Race-prone.

## Repro
1. Connect to cloud device.
2. Call `disconnect()`.
3. Observe `listener` — final two events may arrive in either order: `Disconnected` then `Connecting` (because the inactivity timeout in `WSEventsDeviceMonitor.transformLatest` lapses after `disconnect()` flips state).

## Root Cause
- The disconnect path does not announce intent before action.
- The Cloud monitor's lifecycle is tied to `scope`, not to the api-instance.

## Impact
- UI cannot reliably show "disconnecting" state.
- Misordered final status on cloud — caller may briefly think the device reconnected after they explicitly disconnected.

## Suggested Fix
```kotlin
override suspend fun disconnect() {
    listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnecting)
    monitorJob?.cancelAndJoin()
    httpEngine.close()
    httpEngineOriginal.close()
    listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
}
```

Where `monitorJob` is captured at construction time (`scope.launch { ... }.also { monitorJob = it }`), and the monitor itself uses a child SupervisorJob that gets cancelled here.
