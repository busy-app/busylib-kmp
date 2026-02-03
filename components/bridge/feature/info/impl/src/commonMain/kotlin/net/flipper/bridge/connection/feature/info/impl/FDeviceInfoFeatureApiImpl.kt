package net.flipper.bridge.connection.feature.info.impl

import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.log.LogTagProvider

@Inject
class FDeviceInfoFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
) : FDeviceInfoFeatureApi, LogTagProvider {
    override val TAG = "FDeviceInfoFeatureApi"

    override suspend fun getDeviceInfo(): CResult<BusyBarStatusSystem> {
        return rpcFeatureApi.fRpcSystemApi.getStatusSystem().toCResult()
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
