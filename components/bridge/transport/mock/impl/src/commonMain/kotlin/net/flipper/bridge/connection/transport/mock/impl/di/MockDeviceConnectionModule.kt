package net.flipper.bridge.connection.transport.mock.impl.di

import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import net.flipper.bridge.connection.transport.mock.MockDeviceConnectionApi
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ClassKey

@ContributesTo(BusyLibGraph::class)
interface MockDeviceConnectionModule {
    @IntoMap
    @Provides
    @ClassKey(FMockDeviceConnectionConfig::class)
    fun getMockDeviceConnection(
        mockDeviceConnectionApi: MockDeviceConnectionApi
    ): DeviceConnectionApiHolder {
        return DeviceConnectionApiHolder(
            mockDeviceConnectionApi
        )
    }
}
