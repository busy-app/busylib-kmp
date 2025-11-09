package com.flipperdevices.bridge.connection.configbuilder.impl.builders

import com.flipperdevices.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import me.tatarka.inject.annotations.Inject

@Inject
class BUSYBarMockBuilderConfig {
    fun build() = FMockDeviceConnectionConfig(address = "BUSY Bar")
}
