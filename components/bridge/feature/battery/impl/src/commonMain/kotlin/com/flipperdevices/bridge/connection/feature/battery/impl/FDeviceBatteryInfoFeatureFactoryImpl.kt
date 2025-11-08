package com.flipperdevices.bridge.connection.feature.battery.impl

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureQualifier
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope

@Inject
@FDeviceFeatureQualifier(FDeviceFeature.BATTERY_INFO)
@ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
class FDeviceBatteryInfoFeatureFactoryImpl(
    private val deviceInfoFeatureFactory: FDeviceBatteryInfoFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val rpcFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FRpcFeatureApi::class)
            ?.await()
            ?: return null
        val metaInfoApi = connectedDevice as? FTransportMetaInfoApi
        return deviceInfoFeatureFactory(
            rpcFeatureApi = rpcFeatureApi,
            metaInfoApi = metaInfoApi
        )
    }
}
