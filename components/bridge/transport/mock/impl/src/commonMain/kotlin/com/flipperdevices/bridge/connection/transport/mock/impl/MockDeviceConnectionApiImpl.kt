package com.flipperdevices.bridge.connection.transport.mock.impl

import com.flipperdevices.bridge.connection.transport.common.api.DeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import com.flipperdevices.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.bridge.connection.transport.mock.FMockApi
import com.flipperdevices.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.mock.MockDeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.mock.impl.meta.MockFTransportMetaInfoApiImpl
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesIntoMap
import me.tatarka.inject.annotations.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Inject
@ClassKey(FMockDeviceConnectionConfig::class)
@ContributesIntoMap(BusyLibGraph::class, binding<DeviceConnectionApi<*, *>>())
class MockDeviceConnectionApiImpl : MockDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FMockDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FMockApi> = runCatching {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)
        val mockApi = object : FMockApi, FTransportMetaInfoApi by MockFTransportMetaInfoApiImpl() {
            val bsbMockEngine = getBSBMockHttpEngine()
            override suspend fun disconnect() {
                listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
            }

            override fun getDeviceHttpEngine() = bsbMockEngine
        }
        delay(duration = 2.seconds)
        listener.onStatusUpdate(
            FInternalTransportConnectionStatus.Connected(
                scope = scope,
                deviceApi = mockApi
            )
        )

        return@runCatching mockApi
    }
}
