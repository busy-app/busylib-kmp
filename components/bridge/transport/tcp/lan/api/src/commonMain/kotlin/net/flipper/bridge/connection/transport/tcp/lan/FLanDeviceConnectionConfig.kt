package net.flipper.bridge.connection.transport.tcp.lan

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.core.busylib.data.nonEmptyListOf

data class FLanDeviceConnectionConfig(
    val host: String,
    val name: String
) : FDeviceConnectionConfig<FLanApi>() {
    override fun getTransportTypes() = nonEmptyListOf(FInternalTransportConnectionType.LAN)
}
