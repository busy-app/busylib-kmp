package net.flipper.bridge.connection.feature.firmwareupdate.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbVersionChangelog
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FFirmwareUpdateFeatureApi : FDeviceFeatureApi {
    fun getUpdateStatusFlow(): WrappedFlow<UpdateStatus>
    suspend fun startUpdateCheck(): CResult<Unit>
    suspend fun startVersionInstall(version: String): CResult<Unit>

    /**
     * Force stop firmware download if possible
     */
    suspend fun stopFirmwareUpdate(): CResult<Unit>
    suspend fun getVersionChangelog(version: String): CResult<BsbVersionChangelog>
}
