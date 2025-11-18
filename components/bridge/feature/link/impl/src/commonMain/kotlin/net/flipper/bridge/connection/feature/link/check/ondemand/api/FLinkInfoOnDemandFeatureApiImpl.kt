package net.flipper.bridge.connection.feature.link.check.ondemand.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.link.check.status.api.LinkedAccountInfoApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider

@Inject
class FLinkInfoOnDemandFeatureApiImpl(
    @Assisted private val linkedAccountInfoApi: LinkedAccountInfoApi,
) : FLinkedInfoOnDemandFeatureApi, LogTagProvider {
    override val TAG = "FLinkedInfoFeatureApi"
    override val status: WrappedFlow<LinkedAccountInfo> = linkedAccountInfoApi
        .status
        .filterNotNull()
        .wrap()

    @Inject
    class InternalFactory(
        private val factory: (LinkedAccountInfoApi) -> FLinkInfoOnDemandFeatureApiImpl
    ) {
        operator fun invoke(
            linkedAccountInfoApi: LinkedAccountInfoApi,
        ): FLinkInfoOnDemandFeatureApiImpl = factory(
            linkedAccountInfoApi
        )
    }
}
