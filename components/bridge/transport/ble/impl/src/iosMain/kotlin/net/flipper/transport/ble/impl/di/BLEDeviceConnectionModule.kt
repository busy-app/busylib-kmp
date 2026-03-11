package net.flipper.transport.ble.impl.di

import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.transport.ble.impl.manager.FCentralManagerApi
import net.flipper.transport.ble.impl.manager.FCentralManagerImpl
import platform.CoreBluetooth.CBCentralManager
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.KClass

@ContributesTo(BusyLibGraph::class)
interface BLEDeviceConnectionModule {
    @IntoMap
    @Provides
    fun getBLEDeviceConnection(
        bleDeviceConnectionApi: BleDeviceConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FBleDeviceConnectionConfig::class to DeviceConnectionApiHolder(
            bleDeviceConnectionApi
        )
    }

    @Provides
    @SingleIn(BusyLibGraph::class)
    fun getAppleCentralManager(
        manager: CBCentralManager
    ): FCentralManagerApi = FCentralManagerImpl(manager = manager)
}
