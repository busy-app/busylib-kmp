package net.flipper.bridge.connection.feature.rpc.impl.exposed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FRpcFeatureApiFactoryImpl(
    private val fRpcFeatureFactory: FRpcFeatureApiImpl.InternalFactory,
) : FDeviceFeatureApi.Factory, LogTagProvider {
    override val TAG = "FRpcFeatureApiFactory"

    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        // Wait for authorization
        val unsafeFeatureDeviceApi = unsafeFeatureDeviceApi
            .get(FRpcCriticalFeatureApi::class)
            ?.await() ?: return null

        unsafeFeatureDeviceApi.clientModeApi
            .httpClientModeFlow
            .onEach { mode -> info { "Waiting for default, but receive $mode" } }
            .first { mode -> mode == FRpcClientModeApi.HttpClientMode.DEFAULT }

        info { "Waiting for unsafe feature flow completed" }

        val httpClient = connectedDevice as? FHTTPDeviceApi ?: return null

        return fRpcFeatureFactory.invoke(
            client = getHttpClient(httpClient.getDeviceHttpEngine())
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FRpcFeatureApiComponent {
    @Provides
    @IntoMap
    fun provideFRpcFeatureApiFactory(
        fRpcFeatureApiFactory: FRpcFeatureApiFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.RPC_EXPOSED to fRpcFeatureApiFactory
    }
}
