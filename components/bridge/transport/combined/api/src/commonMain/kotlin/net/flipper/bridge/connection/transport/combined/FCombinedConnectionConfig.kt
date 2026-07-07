package net.flipper.bridge.connection.transport.combined

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.core.busylib.data.NonEmptyList
import net.flipper.core.busylib.data.flatten
import net.flipper.core.busylib.data.map

data class FCombinedConnectionConfig(
    override val uniqueId: String,
    val name: String,
    val connectionConfigs: NonEmptyList<FDeviceConnectionConfig<*>>
) : FDeviceConnectionConfig<FCombinedConnectionApi>() {
    override fun getTransportTypes(): NonEmptyList<FInternalTransportConnectionType> {
        return connectionConfigs.map { it.getTransportTypes() }.flatten()
    }
}
