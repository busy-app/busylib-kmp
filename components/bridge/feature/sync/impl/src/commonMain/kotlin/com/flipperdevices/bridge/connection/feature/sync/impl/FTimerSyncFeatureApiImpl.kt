package com.flipperdevices.bridge.connection.feature.sync.impl

import com.flipperdevices.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import kotlinx.coroutines.CoroutineScope

class FTimerSyncFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    private val scope: CoroutineScope
) : FOnDeviceReadyFeatureApi {
    override suspend fun onReady() {
        // Not implemented
    }

    interface InternalFactory {
        operator fun invoke(
            scope: CoroutineScope
        ): FTimerSyncFeatureApiImpl
    }
}
