package net.flipper.bridge.connection.feature.provider.impl.utils

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
import net.flipper.bridge.connection.device.bsb.api.FBSBDeviceApi
import net.flipper.bridge.connection.device.common.api.FDeviceApi
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus

@Inject
class FDeviceConnectStatusToDeviceApi(
    private val fBSBDeviceApiFactory: FBSBDeviceApi.Factory
) {
    fun get(status: FDeviceConnectStatus.Connected): FDeviceApi {
        return when (status.device) {
            is FDeviceBaseModel.FDeviceBSBModelBLE,
            is FDeviceBaseModel.FDeviceBSBModelBLEiOS,
            is FDeviceBaseModel.FDeviceBSBModelLan,
            is FDeviceBaseModel.FDeviceBSBModelCloud,
            is FDeviceBaseModel.FDeviceBSBModelMock -> fBSBDeviceApiFactory(
                scope = status.scope,
                connectedDevice = status.deviceApi
            )
        }
    }
}
