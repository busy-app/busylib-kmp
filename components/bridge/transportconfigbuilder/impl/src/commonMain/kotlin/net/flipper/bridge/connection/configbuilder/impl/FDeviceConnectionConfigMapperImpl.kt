package net.flipper.bridge.connection.configbuilder.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarBLEBuilderConfig
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarCloudBuilderConfig
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarCombinedBuilderConfig
import net.flipper.bridge.connection.configbuilder.impl.builders.BUSYBarMockBuilderConfig
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.data.map

@Inject
@ContributesBinding(BusyLibGraph::class, binding<FDeviceConnectionConfigMapper>())
class FDeviceConnectionConfigMapperImpl(
    private val mockBuilderConfig: BUSYBarMockBuilderConfig,
    private val bleBuilderConfig: BUSYBarBLEBuilderConfig,
    private val cloudBuilderConfig: BUSYBarCloudBuilderConfig,
    private val busyBarCombinedBuilderConfig: BUSYBarCombinedBuilderConfig
) : FDeviceConnectionConfigMapper {
    override fun getConnectionConfig(device: BUSYBar): FDeviceConnectionConfig<*> {
        return busyBarCombinedBuilderConfig.build(
            uniqueId = device.uniqueId,
            name = device.humanReadableName,
            connectionConfigs = device.connectionWays.map { connectionWay ->
                map(
                    uniqueId = device.uniqueId,
                    hardwareId = device.hardwareId,
                    connectionWay = connectionWay,
                    humanReadableName = device.humanReadableName
                )
            }
        )
    }

    private fun map(
        uniqueId: String,
        hardwareId: String?,
        connectionWay: BUSYBar.ConnectionWay,
        humanReadableName: String
    ): FDeviceConnectionConfig<*> {
        return when (connectionWay) {
            is BUSYBar.ConnectionWay.BLE -> bleBuilderConfig.build(
                uniqueId = uniqueId,
                address = connectionWay.address,
                deviceName = humanReadableName
            )

            is BUSYBar.ConnectionWay.Cloud -> cloudBuilderConfig.build(
                uniqueId = uniqueId,
                name = humanReadableName,
                deviceId = connectionWay.deviceId
            )

            is BUSYBar.ConnectionWay.Lan -> FLanDeviceConnectionConfig(
                uniqueId = uniqueId,
                hardwareId = hardwareId,
                name = humanReadableName
            )

            BUSYBar.ConnectionWay.Mock -> mockBuilderConfig.build(
                uniqueId = uniqueId,
                name = humanReadableName
            )
        }
    }
}
