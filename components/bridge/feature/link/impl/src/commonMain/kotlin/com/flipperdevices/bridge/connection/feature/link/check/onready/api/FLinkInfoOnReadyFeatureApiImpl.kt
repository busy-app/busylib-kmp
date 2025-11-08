package com.flipperdevices.bridge.connection.feature.link.check.onready.api

import com.flipperdevices.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import com.flipperdevices.core.busylib.log.LogTagProvider

class FLinkInfoOnReadyFeatureApiImpl(
    private val fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi,
) : FLinkedInfoOnReadyFeatureApi, LogTagProvider {
    override val TAG = "FLinkedInfoOnReadyFeatureApi"

    override suspend fun onReady() {
        fLinkedInfoOnDemandFeatureApi.tryCheckLinkedInfo()
    }

    fun interface InternalFactory {
        operator fun invoke(
            fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi,
        ): FLinkInfoOnReadyFeatureApiImpl
    }
}
