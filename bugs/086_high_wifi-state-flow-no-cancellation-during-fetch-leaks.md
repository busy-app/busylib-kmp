# `getWifiStateFlow` keeps stale-network entries forever and lacks cancellation between iterations

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/feature/wifi/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/wifi/impl/FWiFiFeatureApiImpl.kt` (lines 63–94)

## Summary
`getWifiStateFlow()` polls every 3 s and emits the union of the previous result with the latest scan: any SSID that was ever observed remains in the list forever, even if it has not been seen in many subsequent scans. Combined with the lack of `pollingDelay` cancellation re-check between fetches, this produces:

1. **Stale ghosts**: an SSID seen once at -90 dBm that has since left range stays in the list with its last RSSI, indefinitely.
2. **Unbounded growth**: in dense areas, the list grows without bound across the lifetime of the subscription.
3. **Race after disconnect**: the `while (isActive) { exponentialRetry { ... } ; delay(...) }` loop holds an `exponentialRetry` that retries forever if the device is gone but the scope hasn't been cancelled yet, blocking termination.

## Repro
```kotlin
val list = mutableListOf<List<WiFiNetwork>>()
val job = launch { fWiFiFeatureApi.getWifiStateFlow().toList(list) }
// Walk past several APs, leave them, observe list never shrinks below the union of all observations.
```

## Root Cause
```kotlin
networks = networks.map { storedNetwork ->
    val updatedNetwork = newNetworkList.find { storedNetwork.ssid == it.ssid }
    if (updatedNetwork != null) {
        newNetworkList.remove(updatedNetwork)
        updatedNetwork
    } else {
        storedNetwork    // <-- stale entry preserved unconditionally
    }
} + newNetworkList
```
- There is no expiration policy: an SSID that disappears from `newNetworkList` is replicated forward forever.
- `exponentialRetry` is `Long.MAX_VALUE` retries by default, and there is no `withTimeout` / `currentCoroutineContext().isActive` check between attempts, so the producer can stall the parent scope on cancellation.

## Impact
- WiFi picker UIs accumulate dozens of dead SSIDs over time.
- Dropped APs can be selected by the user — `connect()` will then fail because the SSID is no longer in range.
- On disconnect, the scope cancellation may be delayed by a long-running RPC retry inside `exponentialRetry`.

## Suggested Fix
- Replace the merge-and-keep behaviour with: emit only the latest scan, OR add a "last seen at" timestamp and evict entries older than e.g. 3 polling cycles.
- Bound `exponentialRetry` (`retries = 3`) and check `currentCoroutineContext().isActive` between iterations.
- Consider exposing two flows: `latestScan: ImmutableList<WiFiNetwork>` (fresh only) and `mergedView` (with TTL) so consumers pick.
