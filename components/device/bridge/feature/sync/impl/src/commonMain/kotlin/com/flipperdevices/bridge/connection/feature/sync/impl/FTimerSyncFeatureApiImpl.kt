package com.flipperdevices.bridge.connection.feature.sync.impl

import com.flipperdevices.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope

@Inject
class FTimerSyncFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    @Assisted private val scope: CoroutineScope
) : FOnDeviceReadyFeatureApi {
    override suspend fun onReady() {
        // Not implemented
    }

    @AssistedFactory
    interface InternalFactory {
        operator fun invoke(
            scope: CoroutineScope
        ): FTimerSyncFeatureApiImpl
    }
}
