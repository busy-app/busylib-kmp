package com.flipperdevices.bridge.connection.feature.battery.impl

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import kotlinx.coroutines.CoroutineScope

class FDeviceBatteryInfoFeatureFactoryImpl(
    private val deviceInfoFeatureFactory: FDeviceBatteryInfoFeatureApiImpl.InternalFactory
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
        val metaInfoApi = connectedDevice as? FTransportMetaInfoApi
        return deviceInfoFeatureFactory(
            rpcFeatureApi = rpcFeatureApi,
            metaInfoApi = metaInfoApi
        )
    }
}
