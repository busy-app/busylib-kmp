package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.fixture.api

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi

class FakeLanApi : FLanApi {
    override val deviceName: String = "TestDevice"

    override suspend fun tryUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() = Unit

    override fun getDeviceHttpEngine(): HttpClientEngine {
        error("Not implemented in test")
    }

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> = flowOf(emptyList())
}
