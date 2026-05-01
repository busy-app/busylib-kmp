# medium — `getCapabilities()` `shareIn(WhileSubscribed())` re-emits stale list and may dispose between subscribers

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FLanApiImpl.kt:41-51`
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FCloudApiImpl.kt:76-82`

## Summary
```kotlin
private val _capabilities = flowOf(listOf(...))
    .shareIn(scope, SharingStarted.WhileSubscribed(), 1)
```

`flowOf(...)` is a one-shot finite flow. `shareIn(WhileSubscribed())` will:
- on first subscription, start collecting `flowOf` → emit the list once → the upstream completes;
- replay-cache holds the value (replay=1);
- when last subscriber leaves, `WhileSubscribed()` cancels the collector and resets;
- next subscription **restarts the upstream**, which is `flowOf`, so the value is re-emitted — fine, but at the cost of work.

A bigger issue: the list is **hard-coded constant** (`flowOf(listOf(BB_WEBSOCKET_SUPPORTED, ...))`) yet wrapped in flow + shareIn machinery. There is no upstream that ever emits a new value. If capabilities are static, this should just be `flowOf(...)` directly (or `MutableStateFlow(constant).asStateFlow()`). The current `shareIn` adds zero value and creates lifecycle issues:

- `scope` may be the connection scope — when the connection dies, even consumers that asked for capabilities recently lose the cached value.
- WhileSubscribed default `stopTimeout = 0`: every time there are zero collectors briefly, the cache is dropped.

For Cloud: `flowOf(listOf<FHTTPTransportCapability>())` — empty list. So the cloud transport reports zero capabilities ALWAYS. Is that intentional? `FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED` etc. ought to be settable based on what the cloud actually supports. As-is, downstream consumers checking capabilities will believe the cloud supports nothing.

## Repro
1. Connect via cloud.
2. Subscribe to `getCapabilities()`. Receive `[]`.
3. Any feature that requires a capability is silently disabled.

## Root Cause
- Flow machinery overengineered for static data.
- Empty cloud capabilities list looks like a TODO that shipped.

## Impact
- Cloud transport silently disables all capability-gated features.
- Wasted resources for static lists.

## Suggested Fix
1. Replace `_capabilities = flowOf(...).shareIn(...)` with `_capabilities = MutableStateFlow(immutable list).asStateFlow()` (and wrap as `WrappedStateFlow` per project rules — see related bug).
2. Populate cloud capabilities accurately (likely the same `BB_WEBSOCKET_SUPPORTED, BB_DOWNLOAD_UPDATE_SUPPORTED` minus `BB_LOCAL_CONNECTION` plus a `BB_CLOUD_CONNECTION` if such an enum exists).
