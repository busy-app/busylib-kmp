# `WS_PING_INTERVAL` is configured twice: in OkHttp engine and in Ktor `WebSockets` plugin

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/commonMain/kotlin/net/flipper/core/ktor/HttpClient.kt` lines 33, 54–57
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/androidMain/kotlin/net/flipper/core/ktor/HttpEnginePlatform.android.kt` line 15
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/jvmMain/kotlin/net/flipper/core/ktor/HttpEnginePlatform.jvm.kt` line 17

## Summary
- The `OkHttpClient` is built with `pingInterval(WS_PING_INTERVAL)`.
- The Ktor `WebSockets` plugin is **also** configured with `pingInterval = WS_PING_INTERVAL`.

These two layers both send pings; in practice they don't deduplicate. OkHttp sends WebSocket pings at the engine layer; Ktor's `WebSockets` plugin schedules its own application-level pings. The result is **double the wire traffic** for keepalive, and pings spaced unevenly.

```kotlin
// HttpClient.kt
install(WebSockets) {
    pingInterval = WS_PING_INTERVAL
    contentConverter = LoggingWebsocketConverter(jsonSerializer)
}

// HttpEnginePlatform.android.kt
preconfigured = OkHttpClient.Builder()
    .pingInterval(WS_PING_INTERVAL)
    .build()
```

On Apple (Darwin engine), only the Ktor-level ping is configured, so the behaviour differs by platform. This makes the BUSY Lib websocket keepalive non-uniform across platforms and prone to subtle reconnect-on-cellular bugs.

## Root Cause
Each layer's ping configuration is independent and the author did not pick one. The Apple engine path doesn't have `OkHttp` so only Ktor pings apply; on JVM/Android both apply.

## Impact
- 2× WebSocket keepalive traffic on Android/JVM.
- Behaviour difference on Apple.
- Cellular battery / data implications.
- Confusing debugging when ping observed at unexpected intervals.

## Suggested Fix
Pick one layer:
- **Recommended**: drop the Ktor `pingInterval = WS_PING_INTERVAL` and rely on engine-level pings on Android/JVM. On Apple, the Darwin engine doesn't expose ping configuration; configure Ktor ping there only.
- Or invert: only Ktor-level pings, drop the OkHttp `pingInterval` from the engine builder.
