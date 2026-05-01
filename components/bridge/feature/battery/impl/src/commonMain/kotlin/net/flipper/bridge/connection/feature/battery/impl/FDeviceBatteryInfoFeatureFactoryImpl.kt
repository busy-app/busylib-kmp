package net.flipper.bridge.connection.feature.battery.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.get
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FDeviceBatteryInfoFeatureFactoryImpl : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val rpcFeatureApi = unsafeFeatureDeviceApi
            .get(FRpcFeatureApi::class)
            ?.await()
            ?: return null
        val eventsApi = unsafeFeatureDeviceApi
            .get(FEventsFeatureApi::class)
            ?.await() ?: return null
        val metaInfoApi = connectedDevice as? FTransportMetaInfoApi
        return FDeviceBatteryInfoFeatureApiImpl(
            rpcFeatureApi = rpcFeatureApi,
            metaInfoApi = metaInfoApi,
            eventsApi = eventsApi,
            scope = scope
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FDeviceBatteryInfoFeatureComponent {
    @Provides
    @IntoMap
    fun provideFDeviceBatteryInfoFeatureFactory(
        fDeviceBatteryInfoFeatureFactory: FDeviceBatteryInfoFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.BATTERY_INFO to fDeviceBatteryInfoFeatureFactory
    }
}
