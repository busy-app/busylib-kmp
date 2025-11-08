package com.flipperdevices.bridge.connection.feature.rpc.impl.critical

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureQualifier
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.impl.util.getHttpClient
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesIntoMap
import me.tatarka.inject.annotations.Inject

import kotlinx.coroutines.CoroutineScope

@Inject
@FDeviceFeatureQualifier(FDeviceFeature.RPC_CRITICAL)
@ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
class FRpcCriticalFeatureFactoryImpl(
    private val fRpcCriticalFeatureApiFactory: FRpcCriticalFeatureApiImpl.InternalFactory,
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val httpClient = connectedDevice as? FHTTPDeviceApi ?: return null
        return fRpcCriticalFeatureApiFactory.invoke(
            client = getHttpClient(httpClient.getDeviceHttpEngine())
        )
    }
}
