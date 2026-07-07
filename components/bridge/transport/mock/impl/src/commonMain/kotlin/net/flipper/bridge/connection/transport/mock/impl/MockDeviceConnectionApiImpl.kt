package net.flipper.bridge.connection.transport.mock.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.mock.FMockApi
import net.flipper.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import net.flipper.bridge.connection.transport.mock.MockDeviceConnectionApi
import net.flipper.bridge.connection.transport.mock.impl.meta.MockFTransportMetaInfoApiImpl
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import kotlin.time.Duration.Companion.seconds

@Inject
@ContributesBinding(BusyLibGraph::class, binding<MockDeviceConnectionApi>())
class MockDeviceConnectionApiImpl : MockDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FMockDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FMockApi> = runSuspendCatching {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting(config.getTransportTypes()))
        var currentConfig = config
        val mockApi = object : FMockApi, FTransportMetaInfoApi by MockFTransportMetaInfoApiImpl() {
            val bsbMockEngine = getBSBMockHttpEngine()
            override val deviceName get() = currentConfig.deviceName

            override val uniqueId: String get() = currentConfig.uniqueId

            override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
                if (config !is FMockDeviceConnectionConfig) {
                    return Result.failure(IllegalArgumentException("Config $config has different type"))
                }
                if (currentConfig == config) {
                    return Result.success(Unit)
                }
                if (currentConfig.copy(deviceName = config.deviceName) == config) {
                    currentConfig = config
                    return Result.success(Unit)
                }
                return Result.failure(IllegalArgumentException("Config $config has different non-name fields"))
            }

            override suspend fun disconnect() {
                listener.onStatusUpdate(
                    FInternalTransportConnectionStatus.Disconnected(
                        FInternalDisconnectedReason.OTHER
                    )
                )
            }

            override fun getDeviceHttpEngine() = bsbMockEngine
        }
        delay(duration = 2.seconds)
        listener.onStatusUpdate(
            FInternalTransportConnectionStatus.Connected(
                scope = scope,
                deviceApi = mockApi,
                connectionType = FInternalTransportConnectionType.BLE
            )
        )

        return@runSuspendCatching mockApi
    }
}
