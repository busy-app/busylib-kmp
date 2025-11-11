package net.flipper.bridge.connection.transport.mock

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

data class FMockDeviceConnectionConfig(
    val address: String
) : FDeviceConnectionConfig<FMockApi>()
