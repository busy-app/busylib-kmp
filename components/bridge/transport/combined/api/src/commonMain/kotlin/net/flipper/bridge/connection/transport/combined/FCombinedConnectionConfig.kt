package net.flipper.bridge.connection.transport.combined

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

data class FCombinedConnectionConfig(
    val address: String,
    val deviceName: String
) : FDeviceConnectionConfig<FCombinedConnectionApi>()