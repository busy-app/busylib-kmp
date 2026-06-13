package net.flipper.bridge.connection.transport.tcp.lan.impl.metainfo

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketOrchestratorApi
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

@AssistedInject
class FCloudStreamingApi(
    @Assisted private val deviceId: Uuid,
    private val orchestrator: CloudWebSocketOrchestratorApi,
) : FStatusStreamingApi, LogTagProvider {
    override val TAG = "FCloudStreamingApi"

    override fun getEvents(): Flow<StatusStreamingEvent> {
        return orchestrator.getEventsFlow(deviceId)
            .mapNotNull { protobuf ->
                runSuspendCatching {
                    StatusStreamingEvent.Protobuf(Base64.decode(protobuf.data))
                }.onFailure {
                    error(it) { "Failure decode ${protobuf.data}" }
                }.getOrNull()
            }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(deviceId: Uuid): FCloudStreamingApi
    }
}
