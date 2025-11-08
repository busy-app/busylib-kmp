package com.flipperdevices.bridge.connection.configbuilder.impl

import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import com.flipperdevices.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import com.flipperdevices.bridge.connection.configbuilder.impl.builders.BUSYBarBLEBuilderConfig
import com.flipperdevices.bridge.connection.configbuilder.impl.builders.BUSYBarMockBuilderConfig
import com.flipperdevices.bridge.connection.transport.common.api.FDeviceConnectionConfig
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject


@Inject
@ContributesBinding(BusyLibGraph::class, FDeviceConnectionConfigMapper::class)
class FDeviceConnectionConfigMapperImpl(
    private val mockBuilderConfig: BUSYBarMockBuilderConfig,
    private val bleBuilderConfig: BUSYBarBLEBuilderConfig
) : FDeviceConnectionConfigMapper {
    override fun getConnectionConfig(device: FDeviceBaseModel): FDeviceConnectionConfig<*> {
        return when (device) {
            is FDeviceBaseModel.FDeviceBSBModelBLE -> bleBuilderConfig.build(device.address)
            is FDeviceBaseModel.FDeviceBSBModelMock -> mockBuilderConfig.build()
        }
    }
}
