# `BUSYBar.connectionWays` initializer throws on deserialization of legitimate-but-empty data

## Type
broken-feature

**Severity:** high

**Files:**
- `components/bridge/config/api/src/commonMain/kotlin/net/flipper/bridge/connection/config/api/model/BUSYBar.kt` (lines 33–41)

## Summary

`BUSYBar` has a `@Transient` property whose initializer asserts that at least one transport is non-null:

```kotlin
@Transient
val connectionWays: NonEmptyList<ConnectionWay> =
    listOfNotNull(lan, cloud, ble, mock).let { list ->
        require(list.isNotEmpty()) {
            "Invalid BUSYBar '$uniqueId': at least one connection way must be present"
        }
        ...
    }
```

The author left a comment on the next line: "Can be crashed on deserialization." That is a known footgun
that has not been fixed.

Because `kotlinx.serialization` invokes the primary constructor when decoding, *any* persisted `BUSYBar`
record where all four transport fields are null (a state that is reachable today: see `BUSYBar.copy(uniqueId,
ble, cloud, lan, mock, ...)` which returns `null` only when *all* are null at copy-time, but cannot prevent
older serialized states from being read this way) will throw `IllegalArgumentException` and propagate up.

When that happens inside `BBConfigSettingsKrateImpl.loader`, it is silently caught by `runCatching { ... }
.getOrNull()` and the entire device list is replaced by `Factory.create()` (see related bug
`critical_silent-config-wipe-on-deserialization-failure.md`).

## Repro

1. Manually persist a JSON entry with all `connection_way_*` fields set to `null` (this is the steady state
   if a future migration drops a transport, or if a partial write was interrupted).
2. App restart calls `decodeFromString`. The `BUSYBar` constructor's `require(...)` triggers.
3. Combined with the silent `runCatching`, the entire saved device list is wiped.

## Root cause

Computing a transient invariant inside the *constructor* of a serializable class. Validation should happen on
the boundary (after deserialization, with a clear migration / quarantine path), not at construction time.

## Impact

- Pairs with the silent-wipe bug to lose all paired devices on a single corrupt entry.
- Even outside that pairing, callers using `kotlinx.serialization` directly (e.g. for cross-process IPC,
  or future cloud-sync of the device list) will see opaque `IllegalArgumentException` failures.

## Suggested fix

- Remove the `require(...)` from the transient initializer. Make `connectionWays` lazily computed and
  return `null` (or an empty-but-typed list) on bad input.
- Validate in the *consumer* layer (e.g. `BBConfigSettingsKrate.loader` filtering out bad entries with
  logging) so corrupt data is recoverable instead of fatal.
- If the project genuinely wants `BUSYBar` to be impossible to construct empty, add a
  `@Serializer(forClass = BUSYBar::class)` custom serializer that surfaces a typed error on decode rather
  than throwing inside `init`.
