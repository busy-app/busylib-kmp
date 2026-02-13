package net.flipper.bsb.watchers.changename

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.settings.api.FSettingsFeatureApi
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class BUSYLibNameWatcher(
    scope: CoroutineScope,
    private val featureProvider: FFeatureProvider,
    private val orchestrator: FDeviceOrchestrator,
    private val persistedStorage: FDevicePersistedStorage
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "BUSYLibNameWatcher"

    private val singleJobScope = scope.asSingleJobScope()


    override fun onLaunch() {
        singleJobScope.launch {
            combine(
                orchestrator.getState(),
                featureProvider.get<FSettingsFeatureApi>()
            ) { state, featureApi ->
                if (state is FDeviceConnectStatus.Connected) {
                    when (featureApi) {
                        FFeatureStatus.NotFound,
                        FFeatureStatus.Retrieving,
                        FFeatureStatus.Unsupported -> flowOf()

                        is FFeatureStatus.Supported<FSettingsFeatureApi> -> {
                            featureApi.featureApi.getDeviceName().map { deviceName ->
                                deviceName to state.device.uniqueId
                            }
                        }
                    }
                } else flowOf()
            }.flatMapLatest { it }
                .collect { (deviceName, deviceId) ->
                    info { "Receive $deviceName for $deviceId" }
                    updateDeviceName(deviceId, deviceName)
                }
        }
    }

    private suspend fun updateDeviceName(deviceId: String, deviceName: String) {
        persistedStorage.updateDevice(deviceId) {
            when (it) {
                is FDeviceBaseModel.FDeviceBSBModelBLE -> FDeviceBaseModel.FDeviceBSBModelBLE(it.address, it.uniqueId, deviceName)
                is FDeviceBaseModel.FDeviceBSBModelBLEiOS -> FDeviceBaseModel.FDeviceBSBModelBLEiOS(it.uniqueId, deviceName)
                is FDeviceBaseModel.FDeviceBSBModelCloud -> FDeviceBaseModel.FDeviceBSBModelCloud(it.authToken, it.host, it.deviceId, it.uniqueId, deviceName)
                is FDeviceBaseModel.FDeviceBSBModelCombined -> FDeviceBaseModel.FDeviceBSBModelCombined(it.uniqueId, deviceName, it.models)
                is FDeviceBaseModel.FDeviceBSBModelLan -> FDeviceBaseModel.FDeviceBSBModelLan(it.host, it.uniqueId, deviceName)
                is FDeviceBaseModel.FDeviceBSBModelMock -> FDeviceBaseModel.FDeviceBSBModelMock(it.uniqueId, deviceName)
            }
        }
    }
}