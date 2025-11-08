package com.flipperdevices.bridge.connection.feature.firmwareupdate.impl

import com.flipperdevices.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.core.busylib.log.LogTagProvider
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.AssistedFactory
import me.tatarka.inject.annotations.Inject

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
