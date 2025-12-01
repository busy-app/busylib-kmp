package net.flipper.bridge.connection.transport.lan

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

data class FLanDeviceConnectionConfig(
    val address: String
) : FDeviceConnectionConfig<FLanApi>()
