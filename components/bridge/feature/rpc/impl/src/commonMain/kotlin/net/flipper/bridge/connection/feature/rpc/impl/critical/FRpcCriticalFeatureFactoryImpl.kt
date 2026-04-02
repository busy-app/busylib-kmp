package net.flipper.bridge.connection.feature.rpc.impl.critical

import kotlinx.coroutines.CoroutineScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureMapKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesTo

@Inject
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

@ContributesTo(BusyLibGraph::class)
interface FRpcCriticalFeatureComponent {
    @Provides
    @IntoMap
    @FDeviceFeatureMapKey(FDeviceFeature.RPC_CRITICAL)
    fun provideFRpcCriticalFeatureFactory(
        fRpcCriticalFeatureFactory: FRpcCriticalFeatureFactoryImpl
    ): FDeviceFeatureApi.Factory {
        return fRpcCriticalFeatureFactory
    }
}
