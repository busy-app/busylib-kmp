# low — `FCloudStreamingApi.getEvents()` silently swallows decoding failures

## Severity
low

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/metainfo/FCloudStreamingApi.kt:25-34`

## Summary
```kotlin
return orchestrator.getEventsFlow(deviceId)
    .mapNotNull { protobuf ->
        runSuspendCatching {
            StatusStreamingEvent.Protobuf(Base64.decode(protobuf.data))
        }.onFailure {
            error(it) { "Failure decode ${protobuf.data}" }
        }.getOrNull()
    }
```

If Base64 decoding fails for a frame, the failure is logged at error level but the consumer never sees it. With `mapNotNull` the bad frame is silently dropped. This is fine for one-off corruption but if the cloud begins consistently sending malformed frames (e.g. a backend update changes encoding from base64 to raw bytes), the entire stream is silently empty.

The downstream `WSEventsDeviceMonitor` then sees no events for >10s and emits `Connecting` indefinitely (see related bug). User sees "Connecting" forever, logs show repeated decode failures, but no error surfaces to the API consumer.

## Repro
1. Send a frame whose `protobuf.data` is not valid Base64.
2. Confirm: log error, but `getEvents()` consumer receives nothing.

## Root Cause
Choice to `getOrNull()` + `mapNotNull` makes the failure invisible in flow contracts.

## Impact
- Bad backends produce silent stalls.
- Hard to diagnose without log access.

## Suggested Fix
Either:
- Promote decode failure to a `StatusStreamingEvent.DecodeError(throwable)` event, OR
- Let the flow throw so monitor logic can react (re-establish the WS).
