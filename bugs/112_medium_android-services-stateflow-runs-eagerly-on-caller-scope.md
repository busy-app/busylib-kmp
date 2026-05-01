# `services` `StateFlow` is eagerly started on the caller's scope and emits before any observer is attached

## Type
infrastructure

**Severity:** medium

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/androidMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/BLEDeviceConnectionApiImpl.kt` (lines 93-107)

## Summary

```kotlin
val services = device.services()
    .map { state ->
        when (state) {
            is RemoteServices.Discovered -> state.services
            is RemoteServices.Failed -> {
                error { "Service discovery failed: ${state.reason}" }
                null
            }
            RemoteServices.Discovering, RemoteServices.Unknown -> null
        }
    }
    .stateIn(scope, SharingStarted.Eagerly, null)
    .wrap()
```

A few real issues:

1. The `RemoteServices.Failed` case returns `null` — *the same value* as
   `Unknown`/`Discovering`. Callers (`SerialApiFactory.build`,
   `AndroidStreamApiFactory.buildStreamingApi`,
   `FTransportMetaInfoApiImpl`) silently retry against a missing
   characteristic forever. There is no exception or any signal that
   discovery failed.

2. `SharingStarted.Eagerly` starts the upstream `device.services()`
   collection on the caller's `scope` immediately. If `connect` fails after
   service discovery starts but before the bleApi is wired up, the upstream
   continues to consume battery / system resources for the lifetime of
   `scope`, even with no consumers.

3. `stateIn` is started inside `connectUnsafe` *before*
   `FAndroidBleApiImpl` is constructed. If the construction throws, the
   eagerly-started `stateIn` is leaked: it never gets a corresponding
   `disconnect` call to cancel it, and the only way to free it is to cancel
   the entire caller scope. (The caller may be a long-lived connection
   manager scope.)

## Reproduction

- For (1): Force a `RemoteServices.Failed`. Observe that the connect
  succeeds (no throw), the consumer waits forever for the serial RX
  characteristic to appear.
- For (3): Throw in any code below `stateIn(...)` (e.g. a future
  refactor); the upstream `device.services()` still runs on `scope`.

## Root cause

The hot/lazy semantics of `stateIn(Eagerly)` were chosen for caching, but
without lifecycle bookkeeping on the failure path.

## Impact

- Discovery failures are silent: connection appears OK but every read /
  write hangs.
- Resource leak in failed connection paths.

## Suggested fix

1. Surface failures: store discovery state as a sealed type, not nullable.
   `null` should mean "not yet discovered"; failure should be its own
   distinct state and an `IllegalStateException` thrown out of
   `connectUnsafe` when observed.
2. Use `SharingStarted.WhileSubscribed()` (or eagerly with try/finally
   that cancels the produced state flow's `Job` if `connectUnsafe`
   throws).
