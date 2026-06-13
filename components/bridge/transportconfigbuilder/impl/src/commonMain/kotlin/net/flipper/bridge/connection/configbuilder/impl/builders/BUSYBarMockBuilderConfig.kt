package net.flipper.bridge.connection.configbuilder.impl.builders

import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.transport.mock.FMockDeviceConnectionConfig

@Inject
class BUSYBarMockBuilderConfig {
    fun build(
        name: String
    ) = FMockDeviceConnectionConfig(
        deviceName = name
    )
}
