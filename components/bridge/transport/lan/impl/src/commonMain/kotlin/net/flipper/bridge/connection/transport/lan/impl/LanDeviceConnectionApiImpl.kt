package net.flipper.bridge.connection.transport.lan.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.lan.FLanApi
import net.flipper.bridge.connection.transport.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.lan.LanDeviceConnectionApi
import net.flipper.bridge.connection.transport.lan.impl.meta.MockFTransportMetaInfoApiImpl
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.time.Duration.Companion.seconds

@Inject
@ContributesBinding(BusyLibGraph::class, LanDeviceConnectionApi::class)
class LanDeviceConnectionApiImpl : LanDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FLanDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FLanApi> = runCatching {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)
        val mockApi = object : FLanApi, FTransportMetaInfoApi by MockFTransportMetaInfoApiImpl() {
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
