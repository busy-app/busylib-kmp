package net.flipper.bridge.connection.feature.link.check.onready.api

import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.core.busylib.log.LogTagProvider

@Inject
class FLinkInfoOnReadyFeatureApiImpl(
    @Assisted private val fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi,
) : FLinkedInfoOnReadyFeatureApi, LogTagProvider {
    override val TAG = "FLinkedInfoOnReadyFeatureApi"

    override suspend fun onReady() {
        fLinkedInfoOnDemandFeatureApi.tryCheckLinkedInfo()
    }

    @Inject
    class InternalFactory(
        private val factory: (FLinkedInfoOnDemandFeatureApi) -> FLinkInfoOnReadyFeatureApiImpl
    ) {
        operator fun invoke(
            fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi,
        ): FLinkInfoOnReadyFeatureApiImpl = factory(fLinkedInfoOnDemandFeatureApi)
    }
}
