# All paired devices silently wiped when persisted config fails to deserialize

## Type
broken-feature

**Severity:** critical

**Files:**
- `components/bridge/config/impl/src/commonMain/kotlin/net/flipper/bridge/connection/config/impl/BBConfigSettingsKrate.kt` (lines 23–35)
- `components/bridge/config/api/src/commonMain/kotlin/net/flipper/bridge/connection/config/api/model/BUSYBar.kt` (lines 33–41)

## Summary

`BBConfigSettingsKrateImpl.loader` decodes the serialized device list with
`runCatching { json.decodeFromString(...) }.getOrNull() ?: Factory.create()`. Any decoding failure causes the
loader to substitute an **empty** `BBConfigSettings`, silently dropping every paired BUSY Bar that was
previously stored on disk. Combined with `BUSYBar`'s `@Transient` `connectionWays` field (which uses
`require(list.isNotEmpty())` in its initializer — a code path the author explicitly flagged as "Can be crashed
on deserialization"), this is a one-shot "lose everything" path that only requires a single corrupted entry.

## Repro

1. Pair two BUSY Bars on a device and ensure the encoded JSON contains entries with all four
   `connection_way_*` fields set to `null` (this can happen with stale data from a partial migration, a
   crash mid-write, or a future serializer change).
2. Restart the app. `decodeFromString` throws because `BUSYBar`'s constructor `require(list.isNotEmpty())`
   triggers when the transient `connectionWays` is built from an all-`null` set of transports.
3. The wrapping `runCatching { ... }.getOrNull() ?: Factory.create()` swallows the exception.
4. `bleConfigKrate` now publishes an empty `BBConfigSettings`, which is then written back via the saver on the
   next transaction, **permanently overwriting the on-disk record**.

## Root cause

```kotlin
loader = {
    observableSettings.toFlowSettings().getStringOrNullFlow(KEY)
        .map { stringValue ->
            if (stringValue.isNullOrBlank()) {
                Factory.create()
            } else {
                runCatching { json.decodeFromString(Serializer, stringValue) }
                    .getOrNull()
                    ?: Factory.create()    // <-- silent reset
            }
        }
}
```

There is no logging, no migration path, and no "quarantine" of the corrupt blob. Coupled with `BUSYBar`'s
`@Transient` initializer that *throws* on edge inputs, even a recoverable error (one bad device entry) burns
the whole device list.

## Impact

- All paired devices vanish after a single decode error. Users have to re-pair every BUSY Bar (BLE + cloud +
  LAN credentials).
- The error is invisible — no log line, no telemetry, no crash. We cannot tell from telemetry whether/when
  this fires in the wild.
- The very next transaction (which `FDevicePersistedStorageImpl.transactionInternal` performs whenever
  *anything* changes) overwrites the original blob with the empty one, eliminating any chance of recovery.

## Suggested fix

- At minimum, log the throwable (`error(t) { "Failed to deserialize device config" }`) and add telemetry.
- Decode entries individually (`List<BUSYBar>`) using `JsonElement.jsonArray.mapNotNull { entry -> runCatching { decode(entry) }.getOrNull() }`
  so that one bad entry only loses one device.
- Move `BUSYBar.connectionWays` out of the constructor or compute it lazily so deserialization cannot throw on
  empty transports — return an empty `NonEmptyList<ConnectionWay>?` and validate in the consumer.
- Before overwriting on save, refuse to write an empty config when the existing on-disk blob is non-empty
  unless an explicit "wipe all" is requested.
