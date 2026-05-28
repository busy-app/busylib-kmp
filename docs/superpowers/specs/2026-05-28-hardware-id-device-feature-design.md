# HardwareId Device Feature — Design

## Goal

Expose the device hardware id (the `serial_number` from `/api/status/device`, local-only)
behind a proper device feature, instead of consumers reaching into the raw
`FRpcFeatureApi.fRpcSystemApi.getDeviceStatus(...)`. Migrate `HardwareIdProvisioningWatcher`
to consume the new feature so it no longer depends on `:bridge:feature:rpc:api`.

## Decisions (from brainstorming)

- **New dedicated feature module** (not an extension of `info`).
- **Name:** `HardwareId`. Module `hardware-id`, api `FHardwareIdFeatureApi`,
  enum `FDeviceFeature.HARDWARE_ID`.
- **API surface:** a flow of the hardware id only —
  `fun getHardwareId(): WrappedFlow<String>` — not the full device-status model.
- **`localOnly` is hardcoded `true`** inside the impl (the only need is the real local
  serial; cloud-proxied values are never wanted here).
- **Migrate the watcher** to the new feature and update its tests/fakes.

## Module: `components/bridge/feature/hardware-id/{api,impl}`

Mirrors the `info` feature layout and the `flipper.multiplatform` / `flipper.anvil-multiplatform`
convention plugins.

### `:api`

`build.gradle.kts` — `flipper.multiplatform` only. Dependencies:
- `api(projects.components.bridge.feature.common.api)`
- `implementation(projects.components.core.wrapper)`
- `implementation(libs.kotlin.coroutines)`

Per the hard rule, the `:api` module must **not** depend on `:bridge:feature:rpc:api`.

`FHardwareIdFeatureApi.kt`:

```kotlin
package net.flipper.bridge.connection.feature.hardwareid.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FHardwareIdFeatureApi : FDeviceFeatureApi {
    fun getHardwareId(): WrappedFlow<String>
}
```

No wrapped model is needed — the value is a primitive `String` (the serial number),
which crosses the Swift boundary cleanly.

### `:impl`

`build.gradle.kts` — `flipper.multiplatform` + `flipper.anvil-multiplatform`. Dependencies
mirror `info/impl`:
- `implementation(projects.components.bridge.feature.hardwareId.api)`
- `implementation(projects.components.core.di)`
- `implementation(projects.components.core.ktx)`
- `implementation(projects.components.core.log)`
- `implementation(projects.components.core.wrapper)`
- `implementation(projects.components.bridge.feature.common.api)`
- `implementation(projects.components.bridge.transport.common.api)`
- `implementation(projects.components.bridge.feature.rpc.api)`
- `implementation(libs.kotlin.coroutines)`

`FHardwareIdFeatureApiImpl.kt` — follows the `info.deviceVersionFlow` idiom: a cold flow that
fetches the device status local-only and emits the serial number, made resilient with
`exponentialRetry` and shared lazily so repeat collectors reuse the last value.

```kotlin
class FHardwareIdFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val scope: CoroutineScope,
) : FHardwareIdFeatureApi, LogTagProvider {
    override val TAG = "FHardwareIdFeatureApi"

    private val hardwareIdFlow: WrappedFlow<String> = flow {
        val hardwareId = exponentialRetry {
            rpcFeatureApi.fRpcSystemApi
                .getDeviceStatus(localOnly = true)
                .map { it.serialNumber }
        }
        emit(hardwareId)
    }.shareIn(scope, SharingStarted.Lazily, replay = 1).wrapFlow()

    override fun getHardwareId(): WrappedFlow<String> = hardwareIdFlow

    @Inject
    class FDeviceFeatureApiFactory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi,
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            return FHardwareIdFeatureApiImpl(fRpcFeatureApi, scope)
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface FFeatureComponent {
        @Provides
        @IntoMap
        fun provideFeatureFactory(
            factory: FDeviceFeatureApiFactory,
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> =
            FDeviceFeature.HARDWARE_ID to factory
    }
}
```

One top-level class per file: the `FDeviceFeatureApiFactory` and `FFeatureComponent` are
nested in `FHardwareIdFeatureApiImpl`, exactly as in `FDeviceInfoFeatureApiImpl`.

## Wiring

- `FDeviceFeature` enum: add `HARDWARE_ID`.
- `settings.gradle.kts`: register
  `:components:bridge:feature:hardware-id:api` and `:...:impl`.
- `entrypoint/build.gradle.kts`:
  - `api(projects.components.bridge.feature.hardwareId.api)`
  - `implementation(projects.components.bridge.feature.hardwareId.impl)`
  - `export(projects.components.bridge.feature.hardwareId.api)` inside the
    `XCFramework("BusyLibKMP")` block.

## Watcher migration — `HardwareIdProvisioningWatcher`

`provisioning/build.gradle.kts`: drop `implementation(projects.components.bridge.feature.rpc.api)`,
add `implementation(projects.components.bridge.feature.hardwareId.api)`.

Watcher changes:
- `featureProvider.get<FRpcFeatureApi>()` → `featureProvider.get<FHardwareIdFeatureApi>()`.
- On `FFeatureStatus.Supported`, collect the feature's flow instead of calling RPC + retrying
  (retry now lives in the feature impl):
  ```kotlin
  is FFeatureStatus.Supported<FHardwareIdFeatureApi> -> {
      rpcApiStatus.featureApi.getHardwareId().collectLatest { hardwareId ->
          onNewHardwareId(hardwareId = hardwareId, uniqueId = device.uniqueId)
      }
  }
  ```
- Rename `onNewDeviceStatus(deviceStatus: BusyBarStatusDevice, ...)` →
  `onNewHardwareId(hardwareId: String, ...)`; replace `deviceStatus.serialNumber` with
  `hardwareId`. Persistence logic (null → set, mismatch → new device + invalidate cloud,
  equal → no-op) is unchanged.
- Remove imports of `FRpcFeatureApi` and `BusyBarStatusDevice`; remove the now-unused
  `exponentialRetry` import if nothing else uses it.

## Testing

The current branch's watcher test/fakes are WIP and do not compile against the real
`FRpcSystemApi` signature, so they are redesigned rather than preserved.

**Watcher tests** (`commonTest`):
- Replace `FakeRpcFeatureApi` + `FakeRpcSystemApi` with a single
  `FakeHardwareIdFeatureApi(private val hardwareIdFlow: WrappedFlow<String>)` returning it
  from `getHardwareId()`.
- `FakeFeatureProvider` and `createSetup` switch their generic from `FRpcFeatureApi` to
  `FHardwareIdFeatureApi`; `deviceStatusResult: Result<BusyBarStatusDevice>` becomes a
  `WrappedFlow<String>` (e.g. `flowOf("SN-123").wrapFlow()` for success,
  `emptyFlow<String>().wrapFlow()` for the "no emission" case).
- Behavioral cases preserved and re-expressed in terms of the emitted hardware id:
  null hardwareId → set; existing matching → no change; existing mismatch → new device +
  cloud invalidate; feature unsupported → no change; disconnected → no change;
  multi-transport preservation; no emission (failure) → no change.

**Feature impl test** (`commonTest` in `hardware-id/impl`): mock `FRpcFeatureApi`
(MockK). Cover: success maps `serialNumber` and the flow emits it; a transient failure
(fail once, then succeed) exercises the `exponentialRetry` retry branch and still emits.
Target 100% coverage of the new impl.

## Out of scope

- Exposing the other `/api/status/device` fields (MACs, OTP) — only the hardware id is needed.
- Any change to the `info` feature or the raw `rpc` feature API.
