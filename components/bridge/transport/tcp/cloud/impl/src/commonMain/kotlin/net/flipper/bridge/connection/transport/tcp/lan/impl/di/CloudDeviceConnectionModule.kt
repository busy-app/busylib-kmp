package net.flipper.bridge.connection.transport.tcp.lan.impl.di

import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.tcp.cloud.api.CloudDeviceConnectionApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ClassKey

@ContributesTo(BusyLibGraph::class)
interface CloudDeviceConnectionModule {
    @IntoMap
    @Provides
    @ClassKey(FCloudDeviceConnectionConfig::class)
    fun getCloudDeviceConnection(
        lanDeviceConnectionApi: CloudDeviceConnectionApi
    ): DeviceConnectionApiHolder {
        return DeviceConnectionApiHolder(
            lanDeviceConnectionApi
        )
    }
}
