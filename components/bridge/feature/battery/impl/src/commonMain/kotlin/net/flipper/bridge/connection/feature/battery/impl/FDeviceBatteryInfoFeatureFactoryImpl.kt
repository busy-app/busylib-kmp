package net.flipper.bridge.connection.feature.battery.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.get
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.busylib.core.di.BusyLibGraph

@Inject
@ContributesIntoMap(BusyLibGraph::class, binding = binding<FDeviceFeatureApi.Factory>())
@FDeviceFeatureKey(FDeviceFeature.BATTERY_INFO)
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
