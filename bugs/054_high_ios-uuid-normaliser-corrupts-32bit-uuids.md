# `normalizeBluetoothUuid` produces an invalid UUID for 32-bit short forms (8 hex chars)

## Type
infrastructure

**Severity:** high

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/BluetoothUuidNormalizer.kt` (lines 38-41)

## Summary

```kotlin
if (withoutDashes.length == SHORT_UUID_LENGTH || withoutDashes.length == LONG_SHORT_UUID_LENGTH) {
    val paddedUuid = withoutDashes.padStart(SHORT_UUID_LENGTH, '0')   // SHORT_UUID_LENGTH = 4
    return "0000$paddedUuid$BLUETOOTH_BASE_UUID_SUFFIX"
}
```

`SHORT_UUID_LENGTH` is `4`. For a *32-bit* short-form UUID (length 8),
`padStart(4, '0')` is a no-op (already ≥ 4), so `paddedUuid.length == 8`.
The resulting string is:

```
"0000XXXXXXXX-0000-1000-8000-00805f9b34fb"
```

which has **12** hex digits before the first dash instead of the required
**8** — `Uuid.parse(...)` will throw, or worse, mismatch every comparison
in `FPeripheralDiscovery` / `FPeripheralValueRouter`.

The Bluetooth SIG 32-bit base format is

```
XXXXXXXX-0000-1000-8000-00805F9B34FB
```

(no leading `0000`). The padding is only needed for 16-bit short forms
(length 4), not 32-bit (length 8).

## Reproduction

Pass any 32-bit Bluetooth SIG service / characteristic UUID (e.g. the
2-byte form widened to 4-byte: `00001234`) through `normalizeBluetoothUuid`,
then compare with `Uuid.parse("00001234-0000-1000-8000-00805f9b34fb")`.
The two strings will not match (12 hex digits vs 8 in the first group).

## Root cause

Both branches of the `||` use `SHORT_UUID_LENGTH` (4) for the pad target.
The 32-bit case should not pad at all (already 8) and **should not**
prepend `"0000"`.

## Impact

If any device UUID is ever delivered by CoreBluetooth in 32-bit short form,
discovery / value routing for that characteristic silently breaks because
the matched-UUID comparisons all fail. Most Apple devices return short
UUIDs in 16-bit form for SIG services so the bug is rarely hit, but it
*will* trigger on any custom 32-bit short form.

## Suggested fix

```kotlin
if (withoutDashes.length == SHORT_UUID_LENGTH) {
    return "0000$withoutDashes$BLUETOOTH_BASE_UUID_SUFFIX"
}
if (withoutDashes.length == LONG_SHORT_UUID_LENGTH) {
    return "$withoutDashes$BLUETOOTH_BASE_UUID_SUFFIX"
}
```

Add a unit test for `"00001234"` and `"AABBCCDD"` — both should produce
parseable, semantically-correct 128-bit UUIDs.
