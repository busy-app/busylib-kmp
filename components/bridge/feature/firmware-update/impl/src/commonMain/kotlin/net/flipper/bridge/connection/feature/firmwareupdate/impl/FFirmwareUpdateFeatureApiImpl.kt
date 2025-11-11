package net.flipper.bridge.connection.feature.firmwareupdate.impl

import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.core.busylib.log.LogTagProvider

@Inject
@Suppress("UnusedPrivateProperty")
class FFirmwareUpdateFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi
) : FFirmwareUpdateFeatureApi, LogTagProvider {
    override val TAG: String = "FFirmwareUpdateFeatureApi"

    override suspend fun beginFirmwareUpdate(): Result<Unit> {
        return runCatching { error("Not implemented yet") }
    }

    override suspend fun stopFirmwareUpdate(): Result<Unit> {
        return runCatching { error("Not implemented yet") }
    }

    @Inject
    class InternalFactory(
        private val factory: (FRpcFeatureApi) -> FFirmwareUpdateFeatureApiImpl
    ) {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FFirmwareUpdateFeatureApiImpl = factory(rpcFeatureApi)
    }
}
