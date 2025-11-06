package com.flipperdevices.bridge.connection.transport.mock

import com.flipperdevices.bridge.connection.transport.common.api.FDeviceConnectionConfig

data class FMockDeviceConnectionConfig(
    val address: String
) : FDeviceConnectionConfig<FMockApi>()
