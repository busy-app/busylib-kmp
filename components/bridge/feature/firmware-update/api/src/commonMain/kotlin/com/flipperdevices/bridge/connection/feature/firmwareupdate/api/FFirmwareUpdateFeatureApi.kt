package com.flipperdevices.bridge.connection.feature.firmwareupdate.api

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi

interface FFirmwareUpdateFeatureApi : FDeviceFeatureApi {
    /**
     * Start firmware download and after automatically install
     */
    suspend fun beginFirmwareUpdate(): Result<Unit>

    /**
     * Force stop firmware download if possible
     */
    suspend fun stopFirmwareUpdate(): Result<Unit>
}
