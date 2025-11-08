package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.busylib.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi

@Suppress("LongParameterList")
class RootModule(
    val bsbBarsApi: BSBBarsApi,
    val scannerModule: ScannerModule,
    val fDeviceHolderFactoryModule: FDeviceHolderFactoryModule,
    val principalApi: BsbUserPrincipalApi,
    val persistedStorage: FDevicePersistedStorage,
) {
    val orchestratorModule = OrchestratorModule(
        fDeviceHolderFactoryModule = fDeviceHolderFactoryModule
    )
    val fOnDeviceReadyModule = FOnDeviceReadyModule()
    val fDeviceFeatureModule = FDeviceFeatureModule(
        bsbUserPrincipalApi = principalApi,
        bsbBarsApi = bsbBarsApi
    )
    val deviceModule = DeviceModule(
        persistedStorage = persistedStorage,
        orchestratorModule = orchestratorModule,
        fOnDeviceReadyModule = fOnDeviceReadyModule,
        fDeviceFeatureModule = fDeviceFeatureModule,
    )
}
