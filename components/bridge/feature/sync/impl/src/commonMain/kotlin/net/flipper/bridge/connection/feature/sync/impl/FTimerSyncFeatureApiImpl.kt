package net.flipper.bridge.connection.feature.sync.impl

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi

@AssistedInject
class FTimerSyncFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    @Assisted private val scope: CoroutineScope
) : FOnDeviceReadyFeatureApi {
    override suspend fun onReady() {
        // Not implemented
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            scope: CoroutineScope
        ): FTimerSyncFeatureApiImpl
    }
}
