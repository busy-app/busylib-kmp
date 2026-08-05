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
import net.flipper.core.busylib.log.verbose
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

@AssistedInject
class FCloudStreamingApi(
    @Assisted private val deviceId: Uuid,
    private val orchestrator: CloudWebSocketOrchestratorApi,
) : FStatusStreamingApi, LogTagProvider {
    override val TAG = "FCloudStreamingApi"

    private fun ByteArray.isNoOpFrame(): Boolean {
        return isEmpty() || all { byte -> byte == 0.toByte() }
    }

    override fun getEvents(): Flow<StatusStreamingEvent> {
        return orchestrator.getEventsFlow(deviceId)
            .mapNotNull { protobuf ->
                val data = runSuspendCatching {
                    Base64.decode(protobuf.data)
                }.onFailure { t ->
                    error(t) { "Failure decode ${protobuf.data}" }
                }.getOrNull() ?: return@mapNotNull null

                if (data.isNoOpFrame()) {
                    verbose { "Skipping ${data.size}B no-op keepalive frame" }
                    return@mapNotNull null
                }
                StatusStreamingEvent.Protobuf(data)
            }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(deviceId: Uuid): FCloudStreamingApi
    }
}
