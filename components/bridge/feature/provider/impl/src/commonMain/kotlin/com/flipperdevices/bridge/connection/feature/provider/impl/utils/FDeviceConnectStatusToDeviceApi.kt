package com.flipperdevices.bridge.connection.feature.provider.impl.utils

import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import com.flipperdevices.bridge.connection.device.bsb.api.FBSBDeviceApi
import com.flipperdevices.bridge.connection.device.common.api.FDeviceApi
import com.flipperdevices.bridge.connection.orchestrator.api.model.FDeviceConnectStatus

class FDeviceConnectStatusToDeviceApi(
    private val fBSBDeviceApiFactory: FBSBDeviceApi.Factory
) {
    fun get(status: FDeviceConnectStatus.Connected): FDeviceApi {
        return when (status.device) {
            is FDeviceBaseModel.FDeviceBSBModelBLE,
            is FDeviceBaseModel.FDeviceBSBModelMock -> fBSBDeviceApiFactory(
                scope = status.scope,
                connectedDevice = status.deviceApi
            )
        }
    }
}
