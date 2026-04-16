package net.flipper.bridge.connection.transport.mock

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.core.busylib.data.nonEmptyListOf

data class FMockDeviceConnectionConfig(
    val deviceName: String
) : FDeviceConnectionConfig<FMockApi>() {
    override fun getTransportTypes() = nonEmptyListOf(FInternalTransportConnectionType.MOCK)
}
