package net.flipper.busylib.di

import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

@ContributesTo(BusyLibGraph::class)
interface WorkaroundBLEConnectionModule {

    @IntoMap
    @Provides
    fun getWorkaroundBLEDeviceConnection(
        bleDeviceConnectionApi: BleDeviceConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FBleDeviceConnectionConfig::class to DeviceConnectionApiHolder(
            bleDeviceConnectionApi
        )
    }
}
