package net.flipper.bridge.connection.configbuilder.impl.builders

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.mock.FMockDeviceConnectionConfig

@Inject
class BUSYBarMockBuilderConfig {
    fun build(
        name: String
    ) = FMockDeviceConnectionConfig(
        deviceName = name
    )
}
