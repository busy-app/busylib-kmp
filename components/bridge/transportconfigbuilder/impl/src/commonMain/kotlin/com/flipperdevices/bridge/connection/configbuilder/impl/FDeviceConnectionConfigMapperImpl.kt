package com.flipperdevices.bridge.connection.configbuilder.impl

import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import com.flipperdevices.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import com.flipperdevices.bridge.connection.configbuilder.impl.builders.BUSYBarBLEBuilderConfig
import com.flipperdevices.bridge.connection.configbuilder.impl.builders.BUSYBarMockBuilderConfig
import com.flipperdevices.bridge.connection.transport.common.api.FDeviceConnectionConfig
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesBinding(BusyLibGraph::class, binding<FDeviceConnectionConfigMapper>())
class FDeviceConnectionConfigMapperImpl(
    private val mockBuilderConfig: BUSYBarMockBuilderConfig,
    private val bleBuilderConfig: BUSYBarBLEBuilderConfig
) : FDeviceConnectionConfigMapper {
    override fun getConnectionConfig(device: FDeviceBaseModel): FDeviceConnectionConfig<*> {
        return when (device) {
            is FDeviceBaseModel.FDeviceBSBModelBLE -> bleBuilderConfig.build(device.address)
            is FDeviceBaseModel.FDeviceBSBModelBLEiOS -> bleBuilderConfig.build(device.uuid)
            is FDeviceBaseModel.FDeviceBSBModelMock -> mockBuilderConfig.build()
        }
    }
}
