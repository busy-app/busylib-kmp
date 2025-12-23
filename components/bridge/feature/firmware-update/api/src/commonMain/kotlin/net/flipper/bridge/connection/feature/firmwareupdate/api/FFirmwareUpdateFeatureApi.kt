package net.flipper.bridge.connection.feature.firmwareupdate.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbVersionChangelog
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FFirmwareUpdateFeatureApi : FDeviceFeatureApi {
    fun getUpdateStatusFlow(): WrappedFlow<UpdateStatus>

    /**
     * Start firmware download and after automatically install
     */
    suspend fun beginFirmwareUpdate(): Result<Unit>

    /**
     * Force stop firmware download if possible
     */
    suspend fun stopFirmwareUpdate(): Result<Unit>
    suspend fun getNextVersionChangelog(): Result<BsbVersionChangelog>
}
