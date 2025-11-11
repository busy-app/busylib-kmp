package net.flipper.bridge.connection.feature.rpc.impl.critical

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

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
    fun provideFRpcCriticalFeatureFactory(
        fRpcCriticalFeatureFactory: FRpcCriticalFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.RPC_CRITICAL to fRpcCriticalFeatureFactory
    }
}
