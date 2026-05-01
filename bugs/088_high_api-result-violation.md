# Public BLE-transport `connect` returns Kotlin `Result<T>` instead of `CResult<T>`

## Type
infrastructure

**Severity:** high

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/ble/api/BleDeviceConnectionApi.kt` (line 12)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/androidMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/BLEDeviceConnectionApiImpl.kt` (lines 47-49)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/BLEDeviceConnectionApiImpl.kt` (lines 49-51)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/androidMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/api/FAndroidBleApiImpl.kt` (lines 88-99) — `tryUpdateConnectionConfig`
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/api/FIOSBleApiImpl.kt` (lines 75-87) — `tryUpdateConnectionConfig`

## Summary

`BleDeviceConnectionApi.connect` is declared in a public `:api` module to
return `kotlin.Result<FBleApi>`. Kotlin's `Result<T>` is an inline
value class — it does not survive interop with Swift / Objective-C. Per
`AGENTS.md`:

> Return `CResult<T>` from `suspend` functions, never Kotlin's inline
> `Result<T>` (it does not cross the Swift boundary).

`tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit>`
on `FBleApi`'s implementations has the same problem — although the
declaration may live in a "common" interface, the resulting symbol still
leaks `Result` into the XCFramework consumer surface.

## Reproduction

Try to `await` `connect(...)` from Swift. SKIE bridges generic `Result`
poorly (the inline value-class is erased to `id` / `KotlinResult`); errors
arrive as `NSException` hits or as `nil`-failures with no payload.

## Root cause

The interface was authored before the `CResult` constraint was applied to
`:api` modules.

## Impact

- Swift consumers cannot reliably distinguish failure cases / extract the
  exception payload from `connect()`.
- Violates an explicit project-wide rule that the `preparing-for-pr-ci-checks`
  detekt step is supposed to catch — search for any custom rule
  enforcing this and verify it actually fires for these symbols (a
  follow-up bug if not).

## Suggested fix

Change the interface signature:

```kotlin
interface BleDeviceConnectionApi : DeviceConnectionApi<FBleApi, FBleDeviceConnectionConfig> {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FBleDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): CResult<FBleApi>
}
```

and update both `BLEDeviceConnectionApiImpl` files to wrap with a
`runSuspendCatching { … }.toCResult()` helper. Apply the same to
`tryUpdateConnectionConfig`.

If the parent interface `DeviceConnectionApi` itself dictates the return
type, that interface must also be updated; this should be coordinated
across all transports.
