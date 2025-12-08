package net.flipper.bridge.connection.configbuilder.impl.builders

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.lan.FLanDeviceConnectionConfig

@Inject
class BUSYBarLanBuilderConfig {
    fun build(
        host: String
    ): FLanDeviceConnectionConfig {
        return FLanDeviceConnectionConfig(host)
    }
}
