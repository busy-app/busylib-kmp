package net.flipper.bridge.connection.feature.sync.impl

import kotlinx.coroutines.CoroutineScope
import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesIntoSet

@Inject
@ContributesIntoSet(BusyLibGraph::class)
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
