package net.flipper.bridge.connection.transport.tcp.cloud.api

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.core.busylib.data.nonEmptyListOf
import kotlin.uuid.Uuid

data class FCloudDeviceConnectionConfig(
    override val uniqueId: String,
    val deviceId: Uuid,
    val name: String
) : FDeviceConnectionConfig<FCloudApi>() {
    override fun getTransportTypes() = nonEmptyListOf(FInternalTransportConnectionType.CLOUD)
}
