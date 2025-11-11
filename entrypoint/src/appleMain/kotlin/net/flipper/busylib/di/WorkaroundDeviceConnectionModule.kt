package net.flipper.busylib.di

import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import net.flipper.bridge.connection.transport.mock.MockDeviceConnectionApi
import net.flipper.busylib.core.di.BusyLibGraph
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
