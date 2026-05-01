# high — `FCloudApiImpl.init { }` launches monitoring coroutine that leaks if construction throws / connect fails

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FCloudApiImpl.kt:47-49`
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/CloudDeviceConnectionApiImpl.kt:23-37`

## Summary
```kotlin
init {
    scope.launch { wsEventsDeviceMonitor.startMonitoring() }
}
```

`FCloudApiImpl` starts background monitoring inside its `init` block, but the surrounding `connect(...)` returns `Result<FCloudApi>` and there is no symmetric "stopMonitoring on failure" path. Compare with the LAN impl where `lanApi.startMonitoring()` is called explicitly from `connect(...)` after construction. The cloud version assumes the caller will eventually call `disconnect()` even if `connect(...)` throws — which contradicts the `runSuspendCatching` outer wrapper that swallows the exception and never returns the `FCloudApi` to the caller.

If construction succeeds but something downstream of `connect(...)` fails (the `runSuspendCatching` block returns `success(lanApi)`, but the caller treats it as failure for unrelated reasons), the launched coroutine is still running on `scope`, holding `httpEngine`, `httpEngineOriginal`, and the WebSocket open. There is no safety-net cancellation.

Additionally:
- `init { scope.launch { ... } }` schedules work on the constructor parameter `scope`. If `scope` is cancelled at the moment of construction (race), `launch` returns an immediately-cancelled job, so monitoring never runs and the device sits in initial state — but `httpEngineOriginal` is still constructed and never closed because `disconnect()` is never called.
- The `init` block runs synchronously; `getPlatformEngineFactory().create()` has already been called. If the next line (`cloudEngineFactory(...)`) throws, the original engine is leaked (no `try/finally`).

## Repro
1. Cancel the scope passed to `connect(...)` immediately after the call returns success.
2. The outer caller never calls `disconnect()` because they think setup failed.
3. `httpEngineOriginal` (a real Ktor engine with a thread pool) leaks.

## Root Cause
Constructor side effects + unmanaged background launch + asymmetric ownership.

## Impact
- Engine + WS leaks under racy connect/cancel.
- Inconsistent behavior between LAN (explicit `startMonitoring()` outside constructor) and Cloud — same patterns should be parallel.

## Suggested Fix
1. Move `scope.launch { wsEventsDeviceMonitor.startMonitoring() }` out of `init`, expose `suspend fun startMonitoring()`, and call it from `CloudDeviceConnectionApiImpl.connect(...)` after construction (mirror of LAN).
2. Wrap construction in `runSuspendCatching` that, on failure, closes already-constructed resources (engine, ws session) before rethrowing.
3. Bind monitor lifetime to the connection: cancel monitor job in `disconnect()` (currently only `httpEngine.close()` is called — the launched monitoring job continues until `scope` itself dies).
