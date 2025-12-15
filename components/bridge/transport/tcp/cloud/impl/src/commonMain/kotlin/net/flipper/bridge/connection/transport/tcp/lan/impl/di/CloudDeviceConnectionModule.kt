package net.flipper.bridge.connection.transport.tcp.lan.impl.di

import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.tcp.cloud.api.CloudDeviceConnectionApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

@ContributesTo(BusyLibGraph::class)
interface CloudDeviceConnectionModule {
    @IntoMap
    @Provides
    fun getLanDeviceConnection(
        lanDeviceConnectionApi: CloudDeviceConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FCloudDeviceConnectionConfig::class to DeviceConnectionApiHolder(
            lanDeviceConnectionApi
        )
    }
}
