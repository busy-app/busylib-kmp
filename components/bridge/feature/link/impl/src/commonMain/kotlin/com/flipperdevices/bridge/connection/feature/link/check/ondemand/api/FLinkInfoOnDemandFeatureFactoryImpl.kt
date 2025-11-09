package com.flipperdevices.bridge.connection.feature.link.check.ondemand.api

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi

import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides
import kotlinx.coroutines.CoroutineScope

@Inject

class FLinkInfoOnDemandFeatureFactoryImpl(
    private val linkedInfoFeatureFactory: FLinkInfoOnDemandFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val fRpcCriticalFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FRpcCriticalFeatureApi::class)
            ?.await()
            ?: return null

        return linkedInfoFeatureFactory(
            rpcFeatureApi = fRpcCriticalFeatureApi,
            scope = scope
        )
    }
}

@ContributesBinding(BusyLibGraph::class)
interface FLinkInfoOnDemandFeatureComponent {
    @Provides
    fun provideFLinkInfoOnDemandFeatureFactory(
        fLinkInfoOnDemandFeatureFactory: FLinkInfoOnDemandFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.LINKED_USER_STATUS to fLinkInfoOnDemandFeatureFactory
    }
}