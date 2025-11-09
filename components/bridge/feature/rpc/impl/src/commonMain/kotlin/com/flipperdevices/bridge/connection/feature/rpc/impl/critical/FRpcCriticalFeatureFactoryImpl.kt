package com.flipperdevices.bridge.connection.feature.rpc.impl.critical

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi

import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.impl.util.getHttpClient
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.IntoMap
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject

class FRpcCriticalFeatureFactoryImpl(
    private val fRpcCriticalFeatureApiFactory: FRpcCriticalFeatureApiImpl.InternalFactory,
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val httpClient = connectedDevice as? FHTTPDeviceApi ?: return null
        return fRpcCriticalFeatureApiFactory.invoke(
            client = getHttpClient(httpClient.getDeviceHttpEngine())
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FRpcCriticalFeatureComponent {
    @Provides
    @IntoMap
    fun provideFRpcCriticalFeatureFactory(
        fRpcCriticalFeatureFactory: FRpcCriticalFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.RPC_CRITICAL to fRpcCriticalFeatureFactory
    }
}
