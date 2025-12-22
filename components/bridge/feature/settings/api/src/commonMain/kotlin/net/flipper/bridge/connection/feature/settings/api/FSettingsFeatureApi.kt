package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightnessInfo
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FSettingsFeatureApi : FDeviceFeatureApi {
    fun getVolumeFlow(): Flow<AudioVolumeInfo>
    fun getBrightnessInfoFlow(): Flow<BsbBrightnessInfo>
    suspend fun setBrightness(
        front: BsbBrightness,
        back: BsbBrightness
    ): Result<Unit>

    suspend fun setVolume(volume: Int): Result<Unit>

    fun getDeviceName(): WrappedFlow<String>
    suspend fun setDeviceName(name: String): Result<Unit>
}
