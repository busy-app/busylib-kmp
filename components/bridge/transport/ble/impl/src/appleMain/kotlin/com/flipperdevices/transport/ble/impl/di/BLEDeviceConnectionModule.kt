package com.flipperdevices.transport.ble.impl.di

import com.flipperdevices.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import com.flipperdevices.busylib.core.di.BusyLibGraph
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

@ContributesTo(BusyLibGraph::class)
interface BLEDeviceConnectionModule {
    @IntoMap
    @Provides
    fun getMockDeviceConnection(
        bleDeviceConnectionApi: BleDeviceConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FBleDeviceConnectionConfig::class to DeviceConnectionApiHolder(
            bleDeviceConnectionApi
        )
    }
}