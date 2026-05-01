# `FlipperScannerImpl.findFlipperDevices` allocates a fresh state per subscriber but uses a shared identity

## Type
infrastructure

**Severity:** low

**Files:**
- `components/bridge/device/firstpair/connection/impl/src/androidMain/kotlin/net/flipper/bridge/impl/scanner/FlipperScannerImpl.kt` (lines 37–63)

## Summary

```kotlin
override fun findFlipperDevices(): Flow<Iterable<DiscoveredBluetoothDevice>> {
    val devices = mutableListOf<DiscoveredBluetoothDeviceImpl>()
    val mutex = Mutex()

    return merge(...).map { ... mutex.withLock { devices.add(...) } ... }
}
```

`devices` and `mutex` are allocated **once per call to `findFlipperDevices`** (good — each subscriber has its
own state). Within a single subscription, multiple emissions can be racing (the `merge` of bonded + scan
flows fires from different upstream coroutines), and the mutex correctly serialises mutations. However:

1. `devices.indexOf(discoveredBluetoothDevice)` uses `DiscoveredBluetoothDeviceImpl.equals`, which has a
   broken `equals/hashCode` contract (see related bug). `indexOf` is `O(n)` and re-runs `equals` on every
   element — for a long-running scanner picking up neighbours, this gets slow.
2. `mutableDevicesList = devices.toList()` allocates a fresh list on every emission — fine for correctness,
   but at scan-burst rates this is a steady allocation pressure.
3. The flow returned has no `flowOn(...)` — the mutex/list operations happen on whatever dispatcher the
   collector is on, which on Android is often the main thread.

## Suggested fix

- Replace the manual de-dup with `distinctBy { it.address }` and a `scan` operator, removing the need for the
  external mutex.
- Or use `MutableMap<String, DiscoveredBluetoothDeviceImpl>` keyed by address for O(1) updates.
- Add a `flowOn(Dispatchers.Default)` or similar.
