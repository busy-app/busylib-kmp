package net.flipper.bridge.connection.transport.tcp.lan

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.core.busylib.data.nonEmptyListOf

data class FLanDeviceConnectionConfig(
    override val uniqueId: String,
    val hardwareId: String?,
    val name: String
) : FDeviceConnectionConfig<FLanApi>() {
    override fun getTransportTypes() = nonEmptyListOf(FInternalTransportConnectionType.LAN)
}
