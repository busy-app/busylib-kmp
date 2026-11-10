package net.flipper.bridge.connection.feature.settings.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.settings.model.BsbBrightness
import net.flipper.bridge.connection.feature.settings.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.settings.model.BsbVolume
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.core.busylib.data.Fraction

interface FSettingsFeatureApi : FDeviceFeatureApi {
    fun getVolumeFlow(): WrappedFlow<BsbVolume>
    fun getBrightnessInfoFlow(): WrappedFlow<BsbBrightnessInfo>
    suspend fun setBrightness(
        value: BsbBrightness,
    ): CResult<Unit>

    suspend fun setVolume(volume: Fraction): CResult<Unit>

    fun getDeviceName(): WrappedStateFlow<String>
    suspend fun setDeviceName(name: String): CResult<Unit>
}
