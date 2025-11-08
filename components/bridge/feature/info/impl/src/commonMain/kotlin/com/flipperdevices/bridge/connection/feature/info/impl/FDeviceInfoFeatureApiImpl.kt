package com.flipperdevices.bridge.connection.feature.info.impl

import com.flipperdevices.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import com.flipperdevices.bridge.connection.feature.info.api.model.BSBDeviceInfo
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi

class FDeviceInfoFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi
) : FDeviceInfoFeatureApi {
    override suspend fun getDeviceInfo(): BSBDeviceInfo? {
        val statusSystem = rpcFeatureApi.getStatusSystem().getOrNull() ?: return null

        return BSBDeviceInfo(
            version = statusSystem.version
        )
    }

    interface InternalFactory {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FDeviceInfoFeatureApiImpl
    }
}
