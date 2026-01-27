package net.flipper.bridge.connection.configbuilder.impl.builders

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

@Inject
class BUSYBarCombinedBuilderConfig {
    fun build(
        name: String,
        connectionConfigs: List<FDeviceConnectionConfig<*>>
    ) = FCombinedConnectionConfig(
        name = name,
        connectionConfigs = connectionConfigs
    )
}
