package com.flipperdevices.bridge.connection.feature.sync.impl

import com.flipperdevices.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesIntoSet
import me.tatarka.inject.annotations.Inject

import kotlinx.coroutines.CoroutineScope

@Inject
@ContributesIntoSet(BusyLibGraph::class, binding<FOnDeviceReadyFeatureApi.Factory>())
class FTimerSyncFactoryImpl(
    private val timerSyncFeatureFactory: FTimerSyncFeatureApiImpl.InternalFactory
) : FOnDeviceReadyFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FOnDeviceReadyFeatureApi? {
        return timerSyncFeatureFactory(
            scope = scope
        )
    }
}
