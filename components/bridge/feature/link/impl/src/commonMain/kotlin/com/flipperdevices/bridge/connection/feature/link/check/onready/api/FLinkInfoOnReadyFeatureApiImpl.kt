package com.flipperdevices.bridge.connection.feature.link.check.onready.api

import com.flipperdevices.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import com.flipperdevices.core.busylib.log.LogTagProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import me.tatarka.inject.annotations.Inject

@Inject
class FLinkInfoOnReadyFeatureApiImpl(
    @Assisted private val fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi,
) : FLinkedInfoOnReadyFeatureApi, LogTagProvider {
    override val TAG = "FLinkedInfoOnReadyFeatureApi"

    override suspend fun onReady() {
        fLinkedInfoOnDemandFeatureApi.tryCheckLinkedInfo()
    }

    @AssistedFactory
    interface InternalFactory {
        operator fun invoke(
            fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi,
        ): FLinkInfoOnReadyFeatureApiImpl
    }
}
