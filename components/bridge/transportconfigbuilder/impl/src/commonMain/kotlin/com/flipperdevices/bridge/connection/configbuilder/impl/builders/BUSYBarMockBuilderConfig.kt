package com.flipperdevices.bridge.connection.configbuilder.impl.builders

import com.flipperdevices.bridge.connection.transport.mock.FMockDeviceConnectionConfig

class BUSYBarMockBuilderConfig {
    fun build() = FMockDeviceConnectionConfig(address = "BUSY Bar")
}
