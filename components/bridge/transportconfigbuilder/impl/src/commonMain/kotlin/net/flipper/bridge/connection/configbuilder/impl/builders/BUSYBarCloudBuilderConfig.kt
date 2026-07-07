package net.flipper.bridge.connection.configbuilder.impl.builders

import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import kotlin.uuid.Uuid

@Inject
class BUSYBarCloudBuilderConfig {
    fun build(
        uniqueId: String,
        name: String,
        deviceId: Uuid
    ): FCloudDeviceConnectionConfig {
        return FCloudDeviceConnectionConfig(
            uniqueId = uniqueId,
            name = name,
            deviceId = deviceId
        )
    }
}
