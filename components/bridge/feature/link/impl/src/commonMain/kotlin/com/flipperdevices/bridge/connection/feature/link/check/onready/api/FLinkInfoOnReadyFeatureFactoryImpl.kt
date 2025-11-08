package com.flipperdevices.bridge.connection.feature.link.check.onready.api

import com.flipperdevices.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

import kotlinx.coroutines.CoroutineScope

@Inject
@ContributesMultibinding(BusyLibGraph::class, FOnDeviceReadyFeatureApi.Factory::class)
class FLinkInfoOnReadyFeatureFactoryImpl(
    private val fLinkInfoOnReadyFeatureApiImpl: FLinkInfoOnReadyFeatureApiImpl.InternalFactory
) : FOnDeviceReadyFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FOnDeviceReadyFeatureApi? {
        val fLinkedInfoOnDemandFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FLinkedInfoOnDemandFeatureApi::class)
            ?.await()
            ?: return null

        return fLinkInfoOnReadyFeatureApiImpl(
            fLinkedInfoOnDemandFeatureApi = fLinkedInfoOnDemandFeatureApi
        )
    }
}
