# `FBleDeviceConnectionConfig.kt` declares three top-level classes (one-class-per-file rule)

## Type
infrastructure

**Severity:** high

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/ble/api/FBleDeviceConnectionConfig.kt` (lines 10-31)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/central/FCentralManagerDelegate.kt` (lines 15-25 declare a sealed class hierarchy with multiple top-level data classes inside, but `FCentralManagerEvent` is itself one class — the file is OK; double-check by detekt)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/androidMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/api/serial/FSerialUnsafeApiImpl.kt` (lines 32-103 + 105-121) — `FSerialUnsafeApiImpl` and a private `Waiter<T>` class share a file.
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/feature/ble/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/ble/api/model/FBleStatus.kt` (sealed interface with nested data objects — OK).

## Summary

`AGENTS.md` says:

> One class per file. No multiple top-level classes in a single Kotlin file.

`FBleDeviceConnectionConfig.kt` contains three independent top-level
data classes: `FBleDeviceConnectionConfig`, `FBleDeviceSerialConfig`,
`FBleDeviceStatusStreamingConfig`. None is nested. Splitting them is
required by the project rule, and these are public types so consumer
imports will need to follow the move.

`FSerialUnsafeApiImpl.kt` declares `FSerialUnsafeApiImpl` and the
`private class Waiter<T>` at the top level — both are top-level types in
the same file. Even though `Waiter` is `private`, the rule does not
exempt visibility.

## Repro

`./gradlew detekt` should fail on the custom "one-class-per-file" rule if
configured, or on a generic detekt rule like `MultipleClassesPerFile`. If
detekt is currently silent, that is itself a regression of the lint setup.

## Root cause

Convenience grouping during initial implementation; not split into
separate files.

## Impact

- Code style / organisational rule violated.
- Makes refactors harder — public types live in a file named after one of
  them.

## Suggested fix

Split into:

- `FBleDeviceConnectionConfig.kt`
- `FBleDeviceSerialConfig.kt`
- `FBleDeviceStatusStreamingConfig.kt`

and move `Waiter` either into its own file (`SubscriptionLatch.kt` /
similar) or replace it altogether — see
`high_android-waiter-not-thread-safe.md` for the recommended replacement.
