package com.flipperdevices.bridge.connection.feature.info.impl

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import kotlinx.coroutines.CoroutineScope

class FDeviceInfoFeatureFactoryImpl(
    private val deviceInfoFeatureFactory: FDeviceInfoFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val rpcFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FRpcFeatureApi::class)
            ?.await()
            ?: return null
        return deviceInfoFeatureFactory(
            rpcFeatureApi = rpcFeatureApi
        )
    }
}
