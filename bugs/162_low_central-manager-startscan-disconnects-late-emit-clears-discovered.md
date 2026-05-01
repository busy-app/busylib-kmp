# `FCentralManager.stopScan` clears `_discoveredStream` but `startScan` keeps stale state from previous session if the BLE stack flickers

## Type
infrastructure

**Severity:** low

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/central/FCentralManager.kt` (lines 147-172)

## Summary

```kotlin
override suspend fun startScan() {
    info { "#startScan delegate=${manager.delegate} rawState=${manager.state} " }
    withTimeoutOrNull(BleConstants.CONNECT_TIME) {
        bleStatusStream.first { it == FBLEStatus.POWERED_ON }
    } ?: run {
        warn { "Cannot start scan with BLE state ${bleStatusStream.first()}" }
        return
    }

    manager.scanForPeripheralsWithServices(
        serviceUUIDs = listOf(CBUUID.UUIDWithString("308A")),
        options = null
    )
    info { "Scan started ${manager.delegate}" }
}

override suspend fun stopScan() {
    info { "#stopScan" }
    if (manager.isScanning()) {
        manager.stopScan()
        _discoveredStream.emit(emptySet())
        info { "Scan stopped" }
    }
}
```

Issues:

1. `startScan` does not clear `_discoveredStream` first. If a previous
   scan ended and `stopScan` was called, the set is empty — fine. But if
   the scan was interrupted by a `POWERED_OFF`/`POWERED_ON` cycle (which
   *does* call `_discoveredStream.emit(emptySet())` in `updateBLEStatus`),
   the set is also empty. So this is generally OK, but:
2. The hardcoded `CBUUID.UUIDWithString("308A")` service ID looks like a
   custom service (BUSY Bar specific) — should probably be a constant
   centralized somewhere with a documented meaning. Magic UUID strings
   in scan filters are footguns: a typo silently makes the SDK ignore
   every advertisement.
3. `withTimeoutOrNull(CONNECT_TIME)` waits up to 30s for `POWERED_ON`
   before refusing to scan — but the docstring of `startScan` doesn't
   say "this may suspend for up to 30 s". Callers expect either an
   immediate fast-fail or a never-resolving suspension.

## Reproduction

Set the `308A` constant to a UUID that doesn't match the device's
advertised services. The SDK silently never discovers anything.

## Root cause

Hardcoded scan-filter constant, undocumented suspension contract.

## Impact

- Maintenance hazard.
- Surprising 30 s blocking call.

## Suggested fix

- Move `308A` into `BleConstants` (or a richer config) with a comment
  pointing at the firmware contract.
- Document or split `startScan` into a suspend variant that throws on
  bad state vs a fire-and-forget that returns immediately.
