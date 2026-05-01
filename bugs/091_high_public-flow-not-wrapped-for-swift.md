# high — Public flows exposed as bare `Flow` / not as `WrappedFlow`

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FLanApiImpl.kt:49-51` (`getCapabilities(): Flow<...>`)
- Same file `:71` (`getEvents()` delegates to bare flow)
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FCloudApiImpl.kt:80-86` (`getCapabilities`, `getEvents`)
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/streaming/FLanStreamingApiImpl.kt:36-43,59`
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/metainfo/FCloudStreamingApi.kt:25-34`
- Underlying interfaces are in `:transport:common:api` and currently declare bare `Flow` — the impls match the contract, but per AGENTS.md the contract is wrong.

## Summary
> "Expose `WrappedFlow<T>` / `WrappedStateFlow<T>` / `WrappedSharedFlow<T>`, never bare `Flow` / `StateFlow` (SKIE `FlowInterop` is intentionally disabled)."

Every public flow in this transport returns bare `kotlinx.coroutines.flow.Flow<T>`. Because SKIE FlowInterop is disabled, Swift consumers cannot collect these flows directly — they receive an opaque coroutine type.

The rule applies to both the `FStatusStreamingApi` interface (`getEvents(): Flow<StatusStreamingEvent>` in `:transport:common:api`) and the LAN/Cloud impl modules' overrides.

## Impact
- Swift cannot subscribe to `getEvents()` / `getCapabilities()` cleanly. Either consumers wrote their own bridge or they're broken.
- `FHTTPDeviceApi.getCapabilities(): Flow<...>` is part of the public consumer contract; failure to wrap means the iOS app cannot react to capability changes.

## Suggested Fix
1. Update common interfaces (`FStatusStreamingApi`, `FHTTPDeviceApi`) to return `WrappedFlow<T>` / `WrappedSharedFlow<T>`.
2. Wrap shared flows: `_capabilities.wrap()` / `eventsFlow.wrap()` (whatever helper the project uses; `core/wrapper` likely has it).
3. Add detekt rule to ban bare `kotlinx.coroutines.flow.{Flow,StateFlow,SharedFlow}` from any signature inside `:api` modules.
