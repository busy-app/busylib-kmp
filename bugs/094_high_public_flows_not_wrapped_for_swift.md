# Public flows in cloud `:api` modules are bare `Flow`/`StateFlow`, breaking SKIE interop

## Severity
high

## Type
infrastructure

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/api/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/BSBWebSocket.kt`
  (`fun getEventsFlow(): Flow<WebSocketEvent>`)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/api/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/CloudWebSocketApi.kt`
  (`fun getWSFlow(): Flow<BSBWebSocket?>`)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/api/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/CloudWebSocketOrchestratorApi.kt`
  (`fun getEventsFlow(cloudId: Uuid): Flow<ProtobufBase64>`)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/api/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/channel/api/BusyFirmwareDirectoryChannelApi.kt`
  (`suspend fun getChannelIdFlow(): StateFlow<BsbFirmwareChannelId>`)

## Summary
Every public flow on a cloud `:api` interface is a bare Kotlin `Flow` or `StateFlow`,
not the project's `WrappedFlow`/`WrappedStateFlow`/`WrappedSharedFlow`. Per
`AGENTS.md` hard rule:
> Expose `WrappedFlow<T>` / `WrappedStateFlow<T>` / `WrappedSharedFlow<T>`, never bare
> `Flow` / `StateFlow` (SKIE `FlowInterop` is intentionally disabled).

`BUSYLibHostApi` already follows the rule — these four interfaces do not.

## Repro
- From Swift, attempt to consume e.g. `cloudWebSocketApi.getWSFlow()`. With SKIE
  `FlowInterop` disabled (per AGENTS.md), the bare `Flow` cannot be projected — the
  function isn't callable in any usable form from Swift.

## Root Cause
- These modules pre-date the WrappedFlow rule, or the rule was applied piecewise and
  these signatures were missed.

## Impact
- Swift consumers (the iOS/macOS XCFramework path) cannot subscribe to live device
  events, the WS instance, the orchestrator's per-bar events, or the firmware
  channel id.
- This is the consumer-visible side of the bug; internally Kotlin code works, so the
  problem is invisible to Android/jvm tests.

## Suggested Fix
- Change return types to `WrappedFlow<...>` / `WrappedStateFlow<...>`.
- In `:impl`, call `.wrap()` (already used by `BUSYLibHostApiStub`).
- For `BusyFirmwareDirectoryChannelApi`, change to
  `fun getChannelIdFlow(): WrappedStateFlow<BsbFirmwareChannelId>` (the `suspend`
  modifier is also unnecessary — the krate is created sync).
- Add the `:api` modules to `entrypoint/build.gradle.kts` `XCFramework` exports if not
  already present.
