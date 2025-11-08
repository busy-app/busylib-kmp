package com.flipperdevices.bridge.connection.feature.wifi.impl

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureQualifier
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

import kotlinx.coroutines.CoroutineScope

@Inject
@FDeviceFeatureQualifier(FDeviceFeature.WIFI)
@ContributesMultibinding(BusyLibGraph::class, FDeviceFeatureApi.Factory::class)
class FWiFiFeatureFactoryImpl(
    private val deviceInfoFeatureFactory: FWiFiFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val fRpcFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FRpcFeatureApi::class)
            ?.await()
            ?: return null

        return deviceInfoFeatureFactory(
            rpcFeatureApi = fRpcFeatureApi
        )
    }
}
