package net.flipper.bridge.connection.transport.mock.impl.di

import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import net.flipper.bridge.connection.transport.mock.MockDeviceConnectionApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

@ContributesTo(BusyLibGraph::class)
interface MockDeviceConnectionModule {
    @IntoMap
    @Provides
    fun getMockDeviceConnection(
        mockDeviceConnectionApi: MockDeviceConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FMockDeviceConnectionConfig::class to DeviceConnectionApiHolder(
            mockDeviceConnectionApi
        )
    }
}
