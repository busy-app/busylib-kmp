package net.flipper.bridge.connection.configbuilder.impl.builders

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig

@Inject
class BUSYBarCloudBuilderConfig {
    fun build(
        authToken: String,
        host: String,
        name: String
    ): FCloudDeviceConnectionConfig {
        return FCloudDeviceConnectionConfig(
            authToken = authToken,
            host = host,
            name = name
        )
    }
}
