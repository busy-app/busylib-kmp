package net.flipper.bridge.connection.feature.battery.impl

import kotlinx.coroutines.CoroutineScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureMapKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesTo

@Inject
class FDeviceBatteryInfoFeatureFactoryImpl(
    private val deviceInfoFeatureFactory: FDeviceBatteryInfoFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val rpcFeatureApi = unsafeFeatureDeviceApi
            .get(FRpcFeatureApi::class)
            ?.await()
            ?: return null
        val metaInfoApi = connectedDevice as? FTransportMetaInfoApi
        return deviceInfoFeatureFactory(
            rpcFeatureApi = rpcFeatureApi,
            metaInfoApi = metaInfoApi
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FDeviceBatteryInfoFeatureComponent {
    @Provides
    @IntoMap
    @FDeviceFeatureMapKey(FDeviceFeature.BATTERY_INFO)
    fun provideFDeviceBatteryInfoFeatureFactory(
        fDeviceBatteryInfoFeatureFactory: FDeviceBatteryInfoFeatureFactoryImpl
    ): FDeviceFeatureApi.Factory {
        return fDeviceBatteryInfoFeatureFactory
    }
}
