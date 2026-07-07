package net.flipper.bridge.connection.configbuilder.impl.builders

import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.core.busylib.data.NonEmptyList

@Inject
class BUSYBarCombinedBuilderConfig {
    fun build(
        uniqueId: String,
        name: String,
        connectionConfigs: NonEmptyList<FDeviceConnectionConfig<*>>
    ) = FCombinedConnectionConfig(
        uniqueId = uniqueId,
        name = name,
        connectionConfigs = connectionConfigs
    )
}
