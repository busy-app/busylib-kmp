# `MultiStreamApiImpl.createCloudStreamFlow` instantiates a new `FEventsFeatureApiImpl` per subscriber

## Type
broken-feature

**Severity:** medium

**Files:**
- `components/tools/multistream/impl/src/commonMain/kotlin/net/flipper/tools/multistream/impl/MultiStreamApiImpl.kt` (lines 63–87)

## Summary

When a remote (cloud) stream is requested, the code does:

```kotlin
private fun createCloudStreamFlow(busyBar: BUSYBar): Flow<MultiStreamState> {
    val cloudId = busyBar.cloud ?: return flowOf(MultiStreamState.Empty)

    return channelFlow {
        val scope: CoroutineScope = this
        val eventsApi = FEventsFeatureApiImpl(
            streamingApi = FCloudStreamingApi(
                deviceId = cloudId.deviceId,
                orchestrator = wsOrchestrator
            ),
            scope = scope,
            TAG = "$TAG-FEventsFeatureApi"
        )

        val screenFlow = eventsApi.getBusyLibUpdateEvents()
            .mapNotNull { it.busyLibUpdateEvent }
            .filterIsInstance<BusyLibUpdateEvent.Frame>()

        ScreenFramesProvider(screenFlow).getScreens().collect {
            send(MultiStreamState.Frame(it))
        }
    }
}
```

Issues:

1. **Per-subscriber `FEventsFeatureApiImpl`** — every `collect` of the returned flow opens a brand-new
   subscription to the cloud WebSocket via a fresh `FCloudStreamingApi`. Two simultaneous subscribers (e.g. a
   list view rendering small thumbnails plus a detail view rendering full size) get two independent cloud
   subscriptions for the same device, doubling network usage and frame delivery work on the cloud relay.
   `FScreenStreamingFeatureApiImpl` (the local-device path) avoids this by sharing
   `ScreenFramesProvider` via a single `feature.busyImageFormatFlow`.

2. **No `wsOrchestrator` lifecycle.** The `FCloudStreamingApi` is constructed with the orchestrator but
   nothing in this scope tells the orchestrator to *open* a session or close it. The implementation must do
   so internally on subscription, but if it does, two subscribers are racing to open/close on the same
   underlying connection.

3. **`MultiStreamState.Empty` is never re-emitted on transport switch.** The outer `flatMapLatest` on
   `orchestrator.getState()` will switch to/from `createCloudStreamFlow` based on `state.deviceOrNull` —
   that's the right idea — but if `busyBar.cloud == null` we return `flowOf(MultiStreamState.Empty)` once
   and never again. Consumers expecting "an Empty signal whenever no cloud is available" get only the first
   one and then nothing.

## Suggested fix

- Share the cloud event stream via a `shareIn(scope, SharingStarted.WhileSubscribed(), 1)` keyed by
  `(busyBar.uniqueId, cloudDeviceId)`.
- Add `conflate()` on the returned flow so slow consumers don't stall the WebSocket reader.
- Re-emit `Empty` on every transport-switch transition rather than once.
