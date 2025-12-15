package net.flipper.bridge.connection.transport.tcp.cloud.api

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

data class FCloudDeviceConnectionConfig(
    val authToken: String,
    val host: String,
    val name: String
) : FDeviceConnectionConfig<FCloudApi>()
