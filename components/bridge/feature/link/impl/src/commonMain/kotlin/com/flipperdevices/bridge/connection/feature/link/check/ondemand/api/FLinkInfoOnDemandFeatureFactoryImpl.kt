package com.flipperdevices.bridge.connection.feature.link.check.ondemand.api

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import kotlinx.coroutines.CoroutineScope

class FLinkInfoOnDemandFeatureFactoryImpl(
    private val linkedInfoFeatureFactory: FLinkInfoOnDemandFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val fRpcCriticalFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FRpcCriticalFeatureApi::class)
            ?.await()
            ?: return null

        return linkedInfoFeatureFactory(
            rpcFeatureApi = fRpcCriticalFeatureApi,
            scope = scope
        )
    }
}
