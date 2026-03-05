package net.flipper.bridge.connection.configbuilder.impl

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarBLEBuilderConfig
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarCloudBuilderConfig
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarCombinedBuilderConfig
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarLanBuilderConfig
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarMockBuilderConfig
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, FDeviceConnectionConfigMapper::class)
class FDeviceConnectionConfigMapperImpl(
    private val mockBuilderConfig: BUSYBarMockBuilderConfig,
    private val bleBuilderConfig: BUSYBarBLEBuilderConfig,
    private val lanBuilderConfig: BUSYBarLanBuilderConfig,
    private val cloudBuilderConfig: BUSYBarCloudBuilderConfig,
    private val busyBarCombinedBuilderConfig: BUSYBarCombinedBuilderConfig
) : FDeviceConnectionConfigMapper {
    override fun getConnectionConfig(device: BUSYBar): FDeviceConnectionConfig<*> {
        return busyBarCombinedBuilderConfig.build(
            name = device.humanReadableName,
            connectionConfigs = device.connectionWays.map { connectionWay ->
                map(
                    connectionWay = connectionWay,
                    humanReadableName = device.humanReadableName
                )
            }
        )
    }

    private fun map(
        connectionWay: BUSYBar.ConnectionWay,
        humanReadableName: String
    ): FDeviceConnectionConfig<*> {
        return when (connectionWay) {
            is BUSYBar.ConnectionWay.BLE -> bleBuilderConfig.build(
                address = connectionWay.address,
                deviceName = humanReadableName
            )

            is BUSYBar.ConnectionWay.Cloud -> cloudBuilderConfig.build(
                name = humanReadableName,
                deviceId = connectionWay.deviceId
            )

            is BUSYBar.ConnectionWay.Lan -> lanBuilderConfig.build(
                host = connectionWay.host,
                name = humanReadableName
            )

            BUSYBar.ConnectionWay.Mock -> mockBuilderConfig.build(
                name = humanReadableName
            )
        }
    }
}
