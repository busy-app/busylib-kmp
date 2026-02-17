package net.flipper.bridge.connection.transport.tcp.cloud.api

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import kotlin.uuid.Uuid

data class FCloudDeviceConnectionConfig(
    val deviceId: Uuid,
    val name: String
) : FDeviceConnectionConfig<FCloudApi>()
