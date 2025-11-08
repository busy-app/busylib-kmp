package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bridge.connection.device.bsb.api.FBSBDeviceApi
import com.flipperdevices.bridge.connection.device.bsb.impl.api.FBSBDeviceApiImpl
import com.flipperdevices.bridge.connection.feature.provider.api.FFeatureProvider
import com.flipperdevices.bridge.connection.feature.provider.impl.api.FFeatureProviderImpl
import com.flipperdevices.bridge.connection.feature.provider.impl.utils.FDeviceConnectStatusToDeviceApi
import com.flipperdevices.bridge.connection.service.api.FConnectionService
import com.flipperdevices.bridge.connection.service.impl.FConnectionServiceImpl
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import kotlinx.coroutines.CoroutineScope

class DeviceModule(
    persistedStorage: FDevicePersistedStorage,
    orchestratorModule: OrchestratorModule,
    fOnDeviceReadyModule: FOnDeviceReadyModule,
    fDeviceFeatureModule: FDeviceFeatureModule
) {
    val connectionService: FConnectionService = FConnectionServiceImpl(
        orchestrator = orchestratorModule.orchestrator,
        fDevicePersistedStorage = persistedStorage
    )
    val deviceApiMapper: FDeviceConnectStatusToDeviceApi = FDeviceConnectStatusToDeviceApi(
        fBSBDeviceApiFactory = object : FBSBDeviceApi.Factory {
            override fun invoke(
                scope: CoroutineScope,
                connectedDevice: FConnectedDeviceApi
            ): FBSBDeviceApi {
                return FBSBDeviceApiImpl(
                    scope = scope,
                    connectedDevice = connectedDevice,
                    onReadyFeaturesApiFactories = fOnDeviceReadyModule.onReadyFeaturesApiFactories,
                    factories = fDeviceFeatureModule.factories
                )
            }
        }
    )

    val featureProvider: FFeatureProvider = FFeatureProviderImpl(
        orchestrator = orchestratorModule.orchestrator,
        deviceApiMapper = deviceApiMapper
    )
}
