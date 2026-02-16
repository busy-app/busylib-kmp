package net.flipper.bridge.connection.configbuilder.impl.builders

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import kotlin.uuid.Uuid

@Inject
class BUSYBarCloudBuilderConfig {
    fun build(
        name: String,
        deviceId: Uuid
    ): FCloudDeviceConnectionConfig {
        return FCloudDeviceConnectionConfig(
            name = name,
            deviceId = deviceId
        )
    }
}
