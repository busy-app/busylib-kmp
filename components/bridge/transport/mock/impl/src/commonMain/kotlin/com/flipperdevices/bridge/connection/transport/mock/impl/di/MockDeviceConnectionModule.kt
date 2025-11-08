package com.flipperdevices.bridge.connection.transport.mock.impl.di

import com.flipperdevices.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import com.flipperdevices.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.mock.MockDeviceConnectionApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.r0adkll.kimchi.annotations.ContributesTo
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
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