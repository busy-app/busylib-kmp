package net.flipper.bridge.connection.configbuilder.impl

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.FDeviceCombined
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
    override fun getConnectionConfig(device: FDeviceCombined): FDeviceConnectionConfig<*> {
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
        device: FDeviceCombined.DeviceModel,
        humanReadableName: String
    ): FDeviceConnectionConfig<*> {
        return when (device) {
            is FDeviceCombined.DeviceModel.FDeviceBSBModelBLE -> bleBuilderConfig.build(
                address = device.address,
                deviceName = humanReadableName
            )

            is FDeviceCombined.DeviceModel.FDeviceBSBModelCloud -> cloudBuilderConfig.build(
                authToken = device.authToken,
                host = device.host,
                name = humanReadableName,
                deviceId = device.deviceId
            )

            is FDeviceCombined.DeviceModel.FDeviceBSBModelLan -> lanBuilderConfig.build(
                host = device.host,
                name = humanReadableName
            )

            FDeviceCombined.DeviceModel.FDeviceBSBModelMock -> mockBuilderConfig.build(
                name = humanReadableName
            )
        }
    }
}
