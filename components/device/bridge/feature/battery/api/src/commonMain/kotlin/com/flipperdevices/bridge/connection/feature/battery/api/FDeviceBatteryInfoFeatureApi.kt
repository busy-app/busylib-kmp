package com.flipperdevices.bridge.connection.feature.battery.api

import com.flipperdevices.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import kotlinx.coroutines.flow.Flow

interface FDeviceBatteryInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceBatteryInfo(): Flow<BSBDeviceBatteryInfo>
}
