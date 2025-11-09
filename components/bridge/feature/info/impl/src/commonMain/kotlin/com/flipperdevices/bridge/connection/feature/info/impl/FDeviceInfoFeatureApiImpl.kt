package com.flipperdevices.bridge.connection.feature.info.impl

import com.flipperdevices.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import com.flipperdevices.bridge.connection.feature.info.api.model.BSBDeviceInfo
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.AssistedFactory
import me.tatarka.inject.annotations.Inject

@Inject
class FDeviceInfoFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi
) : FDeviceInfoFeatureApi {
    override suspend fun getDeviceInfo(): BSBDeviceInfo? {
        val statusSystem = rpcFeatureApi.getStatusSystem().getOrNull() ?: return null

        return BSBDeviceInfo(
            version = statusSystem.version
        )
    }

    @AssistedFactory
    interface InternalFactory {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FDeviceInfoFeatureApiImpl
    }
}
