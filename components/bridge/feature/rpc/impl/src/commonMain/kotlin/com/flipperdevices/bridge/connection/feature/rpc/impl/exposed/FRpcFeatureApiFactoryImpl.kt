package com.flipperdevices.bridge.connection.feature.rpc.impl.exposed

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.impl.util.getHttpClient
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

class FRpcFeatureApiFactoryImpl(
    private val fRpcFeatureFactory: FRpcFeatureApiImpl.InternalFactory,
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        // Wait for authorization
        unsafeFeatureDeviceApi
            .getUnsafe(FRpcCriticalFeatureApi::class)
            ?.await()
            ?.clientModeApi
            ?.httpClientModeFlow
            ?.first { mode -> mode == FRpcClientModeApi.HttpClientMode.DEFAULT }
            ?: return null

        val httpClient = connectedDevice as? FHTTPDeviceApi ?: return null

        return fRpcFeatureFactory.invoke(
            client = getHttpClient(httpClient.getDeviceHttpEngine())
        )
    }
}
