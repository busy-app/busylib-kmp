package net.flipper.bridge.connection.transport.combined.impl.connections.helpers

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability

class HalfInitializedDeviceApi : FConnectedDeviceApi, FHTTPDeviceApi {
    @Suppress("UNCHECKED_CAST")
    private fun <T> notYetAssigned(): T = null as T

    override val deviceName = "HalfInitializedDevice"

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return notYetAssigned()
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
