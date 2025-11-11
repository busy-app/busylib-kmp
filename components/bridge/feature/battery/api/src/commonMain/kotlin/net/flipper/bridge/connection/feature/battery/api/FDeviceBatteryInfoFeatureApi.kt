package net.flipper.bridge.connection.feature.battery.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi

interface FDeviceBatteryInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceBatteryInfo(): Flow<BSBDeviceBatteryInfo>
}
