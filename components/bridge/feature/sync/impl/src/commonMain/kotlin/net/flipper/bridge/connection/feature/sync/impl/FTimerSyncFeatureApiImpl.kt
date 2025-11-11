package net.flipper.bridge.connection.feature.sync.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi

@Inject
class FTimerSyncFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    @Assisted private val scope: CoroutineScope
) : FOnDeviceReadyFeatureApi {
    override suspend fun onReady() {
        // Not implemented
    }

    @Inject
    class InternalFactory(
        private val factory: (CoroutineScope) -> FTimerSyncFeatureApiImpl
    ) {
        operator fun invoke(
            scope: CoroutineScope
        ): FTimerSyncFeatureApiImpl = factory(scope)
    }
}
