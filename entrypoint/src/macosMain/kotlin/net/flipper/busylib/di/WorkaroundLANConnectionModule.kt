package net.flipper.busylib.di

import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.LanDeviceConnectionApi
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ClassKey

@ContributesTo(BusyLibGraph::class)
interface WorkaroundLANConnectionModule {
    @IntoMap
    @Provides
    @ClassKey(FLanDeviceConnectionConfig::class)
    fun getWorkaroundLANDeviceConnection(
        lanDeviceConnectionApi: LanDeviceConnectionApi
    ): DeviceConnectionApiHolder {
        return DeviceConnectionApiHolder(
            lanDeviceConnectionApi
        )
    }
}
