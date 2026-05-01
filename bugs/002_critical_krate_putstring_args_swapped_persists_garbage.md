# Krate `putString` arguments swapped — firmware channel preference is corrupted

## Severity
critical

## Type
broken-feature

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/channel/krate/BsbFirmwareChannelIdKrate.kt` (lines 22-25)

## Summary
`BsbFirmwareChannelIdKrate` writes the firmware-channel preference to disk with the
`Settings.putString(key, value)` arguments swapped. The channel id is stored as the
**key**, and the constant key name `"bsb_firmware_channel_id_key"` is stored as the
**value**. The matching `loader` reads the correct key, so it always falls back to
`DEVELOPMENT`.

```kotlin
saver = { bsbFirmwareChannelId ->
    settings.putString(bsbFirmwareChannelId.id, BSB_FIRMWARE_CHANGELOG_ID_KEY)
}
```

`com.russhwolf.settings.Settings#putString` is declared as
`fun putString(key: String, value: String)` (verified against
`multiplatform-settings 1.3.0`).

## Repro
1. Call `BsbFirmwareChannelIdKrate.set(BsbFirmwareChannelId.RELEASE)` (or whatever the
   krate's mutator is — it goes through `DefaultMutableKrate.saver`).
2. Inspect the underlying preferences/settings store. The keys will contain
   `"release"`/`"development"` mapped to `"bsb_firmware_channel_id_key"`, while the
   real key `"bsb_firmware_channel_id_key"` is never written.
3. Restart the process. `loader` reads `getStringOrNull("bsb_firmware_channel_id_key")`,
   which returns `null`, so `BsbFirmwareChannelId.DEVELOPMENT` is selected.

## Root Cause
Argument order swapped at the `putString` call. Almost certainly a regression where
the original lambda used `(value, key)` style without named arguments.

## Impact
- The user-selected firmware channel is silently lost across app restarts.
- A Release-channel device fleet may inadvertently always be served Development
  firmware, or vice versa.
- Pollutes the Settings storage with an unbounded set of keys (one for every distinct
  channel id ever written by the user), since the channel id is used as the key.

## Suggested Fix
Use named arguments:

```kotlin
saver = { bsbFirmwareChannelId ->
    settings.putString(
        key = BSB_FIRMWARE_CHANGELOG_ID_KEY,
        value = bsbFirmwareChannelId.id
    )
}
```

Add a regression test that does `set(RELEASE) → restart krate → verify get() == RELEASE`.
