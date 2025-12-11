package net.flipper.bridge.connection.feature.wifi.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FWiFiFeatureFactoryImpl(
    private val deviceInfoFeatureFactory: FWiFiFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val fRpcFeatureApi = unsafeFeatureDeviceApi
            .get(FRpcFeatureApi::class)
            ?.await()
            ?: return null

        return deviceInfoFeatureFactory(
            rpcFeatureApi = fRpcFeatureApi
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FWiFiFeatureComponent {
    @Provides
    @IntoMap
    fun provideFWiFiFeatureFactory(
        fWiFiFeatureFactory: FWiFiFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.WIFI to fWiFiFeatureFactory
    }
}
