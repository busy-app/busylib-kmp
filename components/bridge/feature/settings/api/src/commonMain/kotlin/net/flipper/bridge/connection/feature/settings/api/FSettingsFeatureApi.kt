package net.flipper.bridge.connection.feature.settings.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightnessInfo
import net.flipper.busylib.core.wrapper.WrappedSharedFlow

interface FSettingsFeatureApi : FDeviceFeatureApi {
    fun getVolumeFlow(): WrappedSharedFlow<AudioVolumeInfo>
    fun getBrightnessInfoFlow(): WrappedSharedFlow<BsbBrightnessInfo>
    suspend fun setBrightness(
        front: BsbBrightness,
        back: BsbBrightness
    ): Result<Unit>

    suspend fun setVolume(volume: Int): Result<Unit>

    fun getDeviceName(): WrappedSharedFlow<String>
    suspend fun setDeviceName(name: String): Result<Unit>
}
