# `DiscoveredBluetoothDeviceImpl.equals` and `hashCode` violate the Java contract

## Type
infrastructure

**Severity:** high

**Files:**
- `components/bridge/device/firstpair/connection/impl/src/androidMain/kotlin/net/flipper/bridge/impl/scanner/DiscoveredBluetoothDeviceImpl.kt` (lines 34–58)
- `components/bridge/device/firstpair/connection/impl/src/androidMain/kotlin/net/flipper/bridge/impl/scanner/FlipperScannerImpl.kt` (lines 38–63)

## Summary

`DiscoveredBluetoothDeviceImpl` defines:

```kotlin
override fun equals(other: Any?): Boolean = when (other) {
    is Peripheral -> device.address == other.address
    is DiscoveredBluetoothDevice -> device.address == other.address
    else -> false
}
override fun hashCode() = device.hashCode()
```

This breaks the standard `equals`/`hashCode` contract:
- Equality is based on `device.address`, but `hashCode` is based on `device` (likely identity / Nordic
  internal hash). Two `DiscoveredBluetoothDeviceImpl` instances backed by *different* `Peripheral` objects
  with the same MAC will compare equal but produce different hash codes — they cannot be reliably stored in
  any hash-based collection.
- `equals` is asymmetric: `peripheral.equals(impl)` (Nordic's contract) will not return true even though
  `impl.equals(peripheral)` does.

`FlipperScannerImpl.findFlipperDevices()` then uses `devices.indexOf(discoveredBluetoothDevice)` to look up
by MAC, relying on this broken contract. When two distinct `Peripheral` instances appear for the same MAC
(reconnect / new advertising session), the scanner may either fail to find an existing entry (so it adds a
duplicate row) or it may stomp on the wrong entry's `lastScanResult`.

## Repro

1. Scan, observe a BUSY Bar with MAC `AA:BB:...`. Now have the OS recreate the `Peripheral` (turn Bluetooth
   off/on, or roll over the advertising session) so a *new* `Peripheral` instance with the same address is
   emitted.
2. The new `DiscoveredBluetoothDeviceImpl` has the same `address` but a different `device.hashCode()`.
3. Place both into a `HashSet<DiscoveredBluetoothDevice>` (which the public `Iterable<>` API does not promise
   not to happen) — both end up in the set, breaking de-dup expectations.
4. In `FlipperScannerImpl.findFlipperDevices`, the new instance compares equal to the old via `indexOf`, but
   any consumer that builds its own hash-based cache will treat them as distinct.

## Root cause

`hashCode` and `equals` use different identity sources. The intent (per `equals`) appears to be MAC-based
identity; the `hashCode` should match.

## Impact

- Subtle scanner UI bugs (duplicate rows for the same physical device after a reconnect).
- Any downstream `Set<DiscoveredBluetoothDevice>` / `Map<DiscoveredBluetoothDevice, _>` is broken.
- The asymmetric `equals` (Peripheral on one side, impl on the other) violates the equivalence-relation rule
  that the JDK depends on.

## Suggested fix

```kotlin
override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DiscoveredBluetoothDevice) return false
    return address == other.address
}
override fun hashCode() = address.hashCode()
```

Drop the special-cased `Peripheral` branch entirely — it cannot be made symmetric.
