package net.flipper.bridge.connection.feature.screenstreaming.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.hasCapability
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FScreenStreamingFeatureFactoryImpl : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val rpcApi = unsafeFeatureDeviceApi
            .get(FRpcFeatureApi::class)
            ?.await()
            ?: return null
        val isWebSocketSupportedFlow = (connectedDevice as? FHTTPDeviceApi)?.hasCapability(
            FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED
        ) ?: flowOf(false)
        return FScreenStreamingFeatureApiImpl(
            scope,
            rpcApi,
            connectedDevice as? FTransportMetaInfoApi,
            isWebSocketSupportedFlow
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FScreenStreamingFeatureComponent {
    @Provides
    @IntoMap
    fun provideFScreenStreamingFeatureFactory(
        fScreenStreamingFeatureFactory: FScreenStreamingFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.SCREEN_STREAMING to fScreenStreamingFeatureFactory
    }
}
