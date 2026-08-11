package net.flipper.bridge.connection.transport.combined.impl.connections.helpers

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability

class TestHttpConnectedDeviceApi(
    override val deviceName: String,
    private val capabilities: List<FHTTPTransportCapability>
) : FConnectedDeviceApi, FHTTPDeviceApi {
    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return flowOf(capabilities)
    }

    override fun getDeviceHttpEngine(): HttpClientEngine {
        error("Http engine is not used in these tests")
    }

    override suspend fun tryUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun disconnect() = Unit
}
