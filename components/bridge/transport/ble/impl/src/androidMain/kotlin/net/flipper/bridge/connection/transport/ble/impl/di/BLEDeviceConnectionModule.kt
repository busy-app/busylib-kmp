package net.flipper.bridge.connection.transport.ble.impl.di

import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ClassKey

@ContributesTo(BusyLibGraph::class)
interface BLEDeviceConnectionModule {
    @IntoMap
    @Provides
    @ClassKey(FBleDeviceConnectionConfig::class)
    fun getBLEDeviceConnection(
        bleDeviceConnectionApi: BleDeviceConnectionApi
    ): DeviceConnectionApiHolder {
        return DeviceConnectionApiHolder(
            bleDeviceConnectionApi
        )
    }
}
