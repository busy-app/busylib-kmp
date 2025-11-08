package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.ble.impl.FDeviceHolderFactory
import com.flipperdevices.bridge.connection.ble.impl.FDeviceOrchestratorImpl
import com.flipperdevices.bridge.connection.configbuilder.impl.FDeviceConnectionConfigMapperImpl
import com.flipperdevices.bridge.connection.configbuilder.impl.builders.BUSYBarBLEBuilderConfig
import com.flipperdevices.bridge.connection.configbuilder.impl.builders.BUSYBarMockBuilderConfig
import com.flipperdevices.bridge.connection.orchestrator.api.FDeviceOrchestrator

class OrchestratorModule(
    fDeviceHolderFactoryModule: FDeviceHolderFactoryModule
) {
    private val deviceConnectionConfigMapper = FDeviceConnectionConfigMapperImpl(
        mockBuilderConfig = BUSYBarMockBuilderConfig(),
        bleBuilderConfig = BUSYBarBLEBuilderConfig(),
    )
    val orchestrator: FDeviceOrchestrator = FDeviceOrchestratorImpl(
        deviceHolderFactory = fDeviceHolderFactoryModule.deviceHolderFactory,
        deviceConnectionConfigMapper = deviceConnectionConfigMapper
    )
}
