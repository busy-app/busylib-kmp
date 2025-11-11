package net.flipper.bridge.connection.feature.info.impl

import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.info.api.model.BSBDeviceInfo
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi

@Inject
class FDeviceInfoFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi
) : FDeviceInfoFeatureApi {
    override suspend fun getDeviceInfo(): BSBDeviceInfo? {
        val statusSystem = rpcFeatureApi.getStatusSystem().getOrNull() ?: return null

        return BSBDeviceInfo(
            version = statusSystem.version
        )
    }

    @Inject
    class InternalFactory(
        private val factory: (FRpcFeatureApi) -> FDeviceInfoFeatureApiImpl
    ) {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FDeviceInfoFeatureApiImpl = factory(rpcFeatureApi)
    }
}
