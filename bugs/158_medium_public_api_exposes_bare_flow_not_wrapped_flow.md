# `FCombinedConnectionApi` exposes bare `Flow<T>` instead of `WrappedFlow<T>`

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/combined/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/FCombinedConnectionApi.kt`
- (interfaces inherited) `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/serial/FHTTPDeviceApi.kt:14`
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/meta/FTransportMetaInfoApi.kt:9-11`
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/serial/FStatusStreamingApi.kt:11-13`

## Summary
`FCombinedConnectionApi` extends `FHTTPDeviceApi`, `FTransportMetaInfoApi`, and `FStatusStreamingApi`, all of which expose plain `kotlinx.coroutines.flow.Flow<T>` from their public surface (`getCapabilities()`, `get(...)`, `getEvents()`). AGENTS.md "Hard rules for API modules" requires:

> Expose `WrappedFlow<T>` / `WrappedStateFlow<T>` / `WrappedSharedFlow<T>`, never bare `Flow` / `StateFlow` (SKIE `FlowInterop` is intentionally disabled).

Because `FlowInterop` is off, the Swift binding for these methods is broken — they can't be consumed from iOS without manual wrapping.

## Repro
Inspect the generated Apple bindings: `getEvents()` and `getCapabilities()` are unusable on iOS.

## Root Cause
The interfaces in `transport/common/api` define plain `Flow`. The combined api just extends them and inherits the violations.

## Impact
- iOS consumers cannot subscribe to combined-transport events / capabilities / meta-info using the published API surface.
- Hard rule violation per AGENTS.md.

## Suggested Fix
Migrate the inherited interfaces in `transport/common/api` to expose `WrappedFlow` / `WrappedSharedFlow`. Adapt all implementations and tests. Where `FCombinedConnectionApi` overrides them, ensure the wrapper is constructed once (e.g. via a `wrapInWrappedFlow(scope)` helper) so subscribers don't pay re-wrapping cost on every call.
