package net.flipper.bridge.connection.feature.screenstreaming.impl

import kotlinx.coroutines.CoroutineScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.get
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph

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
        val eventsFeatureApi = unsafeFeatureDeviceApi
            .get(FEventsFeatureApi::class)
            ?.await()
            ?: return null
        return FScreenStreamingFeatureApiImpl(
            scope = scope,
            fEventsFeatureApi = eventsFeatureApi,
            rpcApi = rpcApi
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FScreenStreamingFeatureComponent {
    @Provides
    @IntoMap
    @FDeviceFeatureKey(FDeviceFeature.SCREEN_STREAMING)
    fun provideFScreenStreamingFeatureFactory(
        fScreenStreamingFeatureFactory: FScreenStreamingFeatureFactoryImpl
    ): FDeviceFeatureApi.Factory {
        return fScreenStreamingFeatureFactory
    }
}
