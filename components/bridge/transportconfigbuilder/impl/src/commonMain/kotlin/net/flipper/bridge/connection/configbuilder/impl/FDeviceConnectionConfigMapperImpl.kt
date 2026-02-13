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
            connectionConfigs = device.models.map {
                map(
                    device = it,
                    device.humanReadableName
                )
            }
        )
    }

    private fun map(
        device: BUSYBar.ConnectionWayModel,
        humanReadableName: String
    ): FDeviceConnectionConfig<*> {
        return when (device) {
            is BUSYBar.ConnectionWayModel.FConnectionWayBSBModelBLE -> bleBuilderConfig.build(
                address = device.address,
                deviceName = humanReadableName
            )

            is BUSYBar.ConnectionWayModel.FConnectionWayBSBModelCloud -> cloudBuilderConfig.build(
                authToken = device.authToken,
                host = device.host,
                name = humanReadableName,
                deviceId = device.deviceId
            )

            is BUSYBar.ConnectionWayModel.FConnectionWayBSBModelLan -> lanBuilderConfig.build(
                host = device.host,
                name = humanReadableName
            )

            BUSYBar.ConnectionWayModel.FConnectionWayBSBModelMock -> mockBuilderConfig.build(
                name = humanReadableName
            )
        }
    }
}
