package net.flipper.bridge.connection.configbuilder.api

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

interface FDeviceConnectionConfigMapper {
    fun getConnectionConfig(device: BUSYBar): FDeviceConnectionConfig<*>
}
