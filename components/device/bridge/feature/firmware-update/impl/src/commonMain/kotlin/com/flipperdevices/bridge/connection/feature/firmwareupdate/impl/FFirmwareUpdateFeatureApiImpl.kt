package com.flipperdevices.bridge.connection.feature.firmwareupdate.impl

import com.flipperdevices.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.core.log.LogTagProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject

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

    @AssistedFactory
    interface InternalFactory {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FFirmwareUpdateFeatureApiImpl
    }
}
