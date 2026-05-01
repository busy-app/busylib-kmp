# `LoggingWebsocketConverter.isApplicable` returns `true` for every frame, then silently returns `null`

## Type
broken-feature

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/commonMain/kotlin/net/flipper/core/ktor/LoggingWebsocketConverter.kt` lines 38–55

## Summary
`isApplicable(frame)` always returns `true`, but `deserialize(...)` only forwards to the inner JSON delegate **if the delegate itself reports `isApplicable`**:

```kotlin
override suspend fun deserialize(charset, typeInfo, content): Any? {
    val text = … // logs
    if (delegate.isApplicable(content)) {
        return delegate.deserialize(charset, typeInfo, content)
    } else {
        return null                       // ← silent loss
    }
}

override fun isApplicable(frame: Frame): Boolean {
    return true                           // ← lies to the framework
}
```

Ktor uses `isApplicable` to decide *whether to invoke the converter at all*. Reporting `true` here means the converter takes responsibility for any frame Ktor receives, including binary, ping, pong, close. For everything that isn't text/JSON the converter then returns `null` — Ktor treats `null` as "deserialised to null" and silently delivers `null` to the calling collector.

## Repro
1. Send a binary `Frame.Binary` to the WebSocket session that uses this converter.
2. The collector receives `null` instead of an error.

## Root Cause
- `isApplicable` should mirror `delegate.isApplicable(frame)` so Ktor can fall back to other converters or surface an error.
- Returning `null` for "I cannot decode this" is incorrect; Ktor expects the converter to throw `WebsocketConverterNotFoundException` or similar, or to return a meaningful value.

## Impact
- Non-JSON frames are swallowed silently. Server-pushed binary protocol messages would never reach handlers; debugging "missing event" reports would be very hard.
- A misconfigured server sending text/plain frames sees collectors receive `null`, which downstream code (typed `as?`) interprets as "no event", breaking reconnect / heartbeat logic.

## Suggested Fix
Delegate to the inner converter:

```kotlin
override fun isApplicable(frame: Frame): Boolean = delegate.isApplicable(frame)

override suspend fun deserialize(charset, typeInfo, content): Any? {
    // log first
    val text = if (content is Frame.Text) content.readText() else content.toString()
    verbose { "<<<<<<< $text" }
    return delegate.deserialize(charset, typeInfo, content)
}
```
This makes Ktor route only frames the JSON converter actually understands, and surfaces real errors instead of `null`.
