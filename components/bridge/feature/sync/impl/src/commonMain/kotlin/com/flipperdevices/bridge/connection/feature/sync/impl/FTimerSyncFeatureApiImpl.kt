package com.flipperdevices.bridge.connection.feature.sync.impl

import com.flipperdevices.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import kotlinx.coroutines.CoroutineScope

@Inject
class FTimerSyncFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    @Assisted private val scope: CoroutineScope
) : FOnDeviceReadyFeatureApi {
    override suspend fun onReady() {
        // Not implemented
    }

    @Inject
    abstract class InternalFactory(
        protected val factory: (CoroutineScope) -> FTimerSyncFeatureApiImpl
    ) {
        operator fun invoke(
            scope: CoroutineScope
        ): FTimerSyncFeatureApiImpl = factory(scope)
    }
}
