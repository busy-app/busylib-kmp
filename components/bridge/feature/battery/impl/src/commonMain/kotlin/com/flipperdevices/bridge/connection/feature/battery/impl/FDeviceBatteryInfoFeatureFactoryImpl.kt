package com.flipperdevices.bridge.connection.feature.battery.impl

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi

import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.IntoMap
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

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
