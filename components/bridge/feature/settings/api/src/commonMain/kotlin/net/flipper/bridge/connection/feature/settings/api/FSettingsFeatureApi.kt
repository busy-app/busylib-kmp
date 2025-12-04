package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo

interface FSettingsFeatureApi : FDeviceFeatureApi {
    fun getVolumeFlow(): Flow<AudioVolumeInfo>
    fun getBrightnessInfoFlow(): Flow<DisplayBrightnessInfo>
}
