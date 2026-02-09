package net.flipper.bridge.connection.configbuilder.impl

import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
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
    override fun getConnectionConfig(device: FDeviceBaseModel): FDeviceConnectionConfig<*> {
        return when (device) {
            is FDeviceBaseModel.FDeviceBSBModelBLE -> bleBuilderConfig.build(
                address = device.address,
                deviceName = device.humanReadableName
            )

            is FDeviceBaseModel.FDeviceBSBModelBLEiOS -> bleBuilderConfig.build(
                address = device.uniqueId,
                deviceName = device.humanReadableName
            )

            is FDeviceBaseModel.FDeviceBSBModelMock -> mockBuilderConfig.build(
                address = device.uniqueId,
                name = device.humanReadableName
            )

            is FDeviceBaseModel.FDeviceBSBModelLan -> lanBuilderConfig.build(
                host = device.host,
                name = device.humanReadableName
            )

            is FDeviceBaseModel.FDeviceBSBModelCloud -> cloudBuilderConfig.build(
                authToken = device.authToken,
                host = device.host,
                name = device.humanReadableName,
                deviceId = device.deviceId
            )

            is FDeviceBaseModel.FDeviceBSBModelCombined -> busyBarCombinedBuilderConfig.build(
                name = device.humanReadableName,
                connectionConfigs = device.models.map {
                    getConnectionConfig(it)
                }
            )
        }
    }
}
