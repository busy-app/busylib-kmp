package net.flipper.bridge.connection.transport.lan.impl.di

import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.lan.LanDeviceConnectionApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

@ContributesTo(BusyLibGraph::class)
interface LanDeviceConnectionModule {
    @IntoMap
    @Provides
    fun getLanDeviceConnection(
        lanDeviceConnectionApi: LanDeviceConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FLanDeviceConnectionConfig::class to DeviceConnectionApiHolder(
            lanDeviceConnectionApi
        )
    }
}
