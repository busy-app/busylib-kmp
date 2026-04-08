package net.flipper.bridge.connection.transport.tcp.lan.impl.metainfo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.JsonPrimitive
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketBarsApi
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

@AssistedInject
class FCloudStreamingApi(
    @Assisted private val deviceId: Uuid,
    private val webSocketBarsApi: CloudWebSocketBarsApi,
) : FStatusStreamingApi, LogTagProvider {
    override val TAG = "FCloudStreamingApi"

    override fun getEvents(): Flow<StatusStreamingEvent> {
        return webSocketBarsApi.getWSFlow().flatMapLatest { ws ->
            if (ws == null) {
                flowOf()
            } else {
                getStatusStreamingFlow(ws)
            }
        }
    }

    private fun getStatusStreamingFlow(webSocket: BSBWebSocket): Flow<StatusStreamingEvent> {
        return webSocket.getEventsFlow().filter { it.barId == deviceId }
            .flatMapConcat { list ->
                list.values.toList().asFlow()
            }.mapNotNull { (key, value) ->
                if (key == "state" && value is JsonPrimitive) {
                    runSuspendCatching {
                        StatusStreamingEvent.Protobuf(Base64.decode(value.content))
                    }.onFailure {
                        error(it) { "Failure decode $value" }
                    }.getOrNull()
                } else {
                    null
                }
            }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(deviceId: Uuid): FCloudStreamingApi
    }
}
