package net.flipper.bridge.connection.configbuilder.impl.builders

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig

@Inject
class BUSYBarCloudBuilderConfig {
    fun build(
        authToken: String,
        name: String
    ): FLanDeviceConnectionConfig {
        return FCloudDeviceConnectionConfig(host, name)
    }
}