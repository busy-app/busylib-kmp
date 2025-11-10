package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import com.flipperdevices.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.mock.MockDeviceConnectionApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

@Suppress("ForbiddenComment")
// TODO: remove me
// This is dirty workaround because kotlin-inject doesn't support one-line multibinding
@ContributesTo(BusyLibGraph::class)
interface WorkaroundDeviceConnectionModule {
    @IntoMap
    @Provides
    fun getWorkaroundMockDeviceConnection(
        mockDeviceConnectionApi: MockDeviceConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FMockDeviceConnectionConfig::class to DeviceConnectionApiHolder(
            mockDeviceConnectionApi
        )
    }
}
