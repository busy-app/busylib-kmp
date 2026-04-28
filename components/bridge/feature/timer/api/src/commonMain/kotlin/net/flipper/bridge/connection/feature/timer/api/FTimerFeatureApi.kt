package net.flipper.bridge.connection.feature.timer.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.timer.api.model.BusyProfileSlot
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FTimerFeatureApi : FDeviceFeatureApi {
    fun getSnapshotsFlow(): WrappedFlow<String>
    suspend fun setSnapshot(rawJson: String): CResult<Unit>
    suspend fun setProfile(slot: BusyProfileSlot, rawJson: String): CResult<Unit>
}
