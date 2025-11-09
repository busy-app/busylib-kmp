package com.flipperdevices.bridge.connection.feature.info.impl

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi

import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides

import kotlinx.coroutines.CoroutineScope

@Inject

class FDeviceInfoFeatureFactoryImpl(
    private val deviceInfoFeatureFactory: FDeviceInfoFeatureApiImpl.InternalFactory
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
        return deviceInfoFeatureFactory(
            rpcFeatureApi = rpcFeatureApi
        )
    }
}

@ContributesBinding(BusyLibGraph::class)
interface FDeviceInfoFeatureComponent {
    @Provides
    fun provideFDeviceInfoFeatureFactory(
        fDeviceInfoFeatureFactory: FDeviceInfoFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.DEVICE_INFO to fDeviceInfoFeatureFactory
    }
}
