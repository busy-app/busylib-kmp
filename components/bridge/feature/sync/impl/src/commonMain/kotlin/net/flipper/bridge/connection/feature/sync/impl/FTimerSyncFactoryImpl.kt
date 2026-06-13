package net.flipper.bridge.connection.feature.sync.impl

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph

@Inject
@ContributesIntoSet(BusyLibGraph::class, binding = binding<FOnDeviceReadyFeatureApi.Factory>())
class FTimerSyncFactoryImpl(
    private val timerSyncFeatureFactory: FTimerSyncFeatureApiImpl.Factory
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
