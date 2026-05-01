# Screen streaming has no backpressure or frame-drop policy

## Type
lack-of-feature

**Severity:** medium

**Files:**
- `components/bridge/feature/screen-streaming/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/screenstreaming/impl/FScreenStreamingFeatureApiImpl.kt`
- `components/bridge/feature/screen-streaming/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/screenstreaming/impl/delegates/ScreenFramesProvider.kt`
- `components/tools/multistream/impl/src/commonMain/kotlin/net/flipper/tools/multistream/impl/MultiStreamApiImpl.kt`

## Summary

`busyImageFormatFlow` is a plain `Flow<BusyImageFormat>` derived from `screenFlow.mapNotNull { mapFrame(it) }`
with no buffering or conflation. `MultiStreamApiImpl.get(...)` similarly forwards frames through `flatMapLatest
{ status.featureApi.busyImageFormatFlow.map { MultiStreamState.Frame(it) } }` and through a `channelFlow { ...
send(MultiStreamState.Frame(it)) }` for the cloud path.

There is no `buffer(...)`, `conflate()`, or explicit drop policy. Each frame is fully decoded (allocating a
~3.4 KB ByteArray, plus ~55 KB of garbage from the boxed `mutableListOf<Byte>` — see related issue) before
the consumer signals readiness. If the consumer is slow (UI thread busy, screenshot-encoding observer
attached, etc.):

- The upstream BLE / WebSocket producer suspends, applying backpressure all the way back to the device's
  notification queue. On Nordic BLE, that queue is bounded; sustained slow consumers cause the device to
  *drop* notifications silently — but the SDK never sees the gap.
- For the cloud path (`channelFlow { send(...) }`), `send` suspends; the WebSocket reader for the cloud is
  blocked behind it, eventually causing the cloud relay to time out and drop the connection.
- A momentarily slow consumer (e.g. UI on a janky frame) can cause the on-device frame producer to be
  starved and the screen to freeze.

A 30 FPS stream of 3.4 KB frames is small in absolute terms, but every consumer-side slowness manifests as
either dropped frames *on the device* (invisible to us) or as a stalled WebSocket.

## Repro

1. Subscribe to `MultiStreamApi.get(busyBar)` from a UI that takes 100 ms to render each frame (simulate
   with `delay(100)` in the collector).
2. The cloud WebSocket (or BLE) starts dropping frames silently. There is no log line to indicate this; the
   device sees backpressure, decides to stop emitting, and the stream eventually dies.

## Root cause

No backpressure policy on the public flow. Screen frames are inherently lossy — old frames are useless once
a newer one is available — so `conflate()` is the obvious fit, but it is not used.

## Impact

- Sluggish UI translates into a stalled stream rather than dropped frames; UX feels broken.
- For long sessions, accumulated backpressure can OOM a slow consumer if upstream is buffered (e.g. through
  the BLE notification queue on iOS, which Nordic buffers internally).

## Suggested fix

Apply `conflate()` (drop old, keep newest) at the public boundary of `FScreenStreamingFeatureApiImpl
.busyImageFormatFlow`:

```kotlin
override val busyImageFormatFlow = screenFramesProvider.getScreens().conflate().wrap()
```

For the cloud path in `MultiStreamApiImpl.createCloudStreamFlow`, use
`channelFlow { ... }.buffer(0, BufferOverflow.DROP_OLDEST)` so a slow consumer does not stall the WebSocket
reader.
