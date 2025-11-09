package com.flipperdevices.bridge.connection.feature.rpc.impl.exposed

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi

import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.impl.util.getHttpClient
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.IntoMap
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject

class FRpcFeatureApiFactoryImpl(
    private val fRpcFeatureFactory: FRpcFeatureApiImpl.InternalFactory,
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        // Wait for authorization
        unsafeFeatureDeviceApi
            .getUnsafe(FRpcCriticalFeatureApi::class)
            ?.await()
            ?.clientModeApi
            ?.httpClientModeFlow
            ?.first { mode -> mode == FRpcClientModeApi.HttpClientMode.DEFAULT }
            ?: return null

        val httpClient = connectedDevice as? FHTTPDeviceApi ?: return null

        return fRpcFeatureFactory.invoke(
            client = getHttpClient(httpClient.getDeviceHttpEngine())
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FRpcFeatureApiComponent {
    @Provides
    @IntoMap
    fun provideFRpcFeatureApiFactory(
        fRpcFeatureApiFactory: FRpcFeatureApiFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.RPC_EXPOSED to fRpcFeatureApiFactory
    }
}
