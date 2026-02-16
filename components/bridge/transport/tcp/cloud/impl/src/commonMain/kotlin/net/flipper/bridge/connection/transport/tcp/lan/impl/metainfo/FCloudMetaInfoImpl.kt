package net.flipper.bridge.connection.transport.tcp.lan.impl.metainfo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketBarsApi
import kotlin.uuid.Uuid

internal typealias FCloudMetaInfoFactory = (Uuid) -> FCloudMetaInfoImpl

@Inject
class FCloudMetaInfoImpl(
    @Assisted private val deviceId: Uuid,
    private val webSocketBarsApi: CloudWebSocketBarsApi,
) : FTransportMetaInfoApi {
    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>> {
        return webSocketBarsApi.getWSFlow()
            .map { ws ->
                if (ws == null) {
                    Result.failure(RuntimeException("Websocket is null"))
                } else {
                    Result.success(getMetaInfoFlow(ws))
                }
            }
    }

    private fun getMetaInfoFlow(webSocket: BSBWebSocket): Flow<TransportMetaInfoData> {
        return webSocket.getEventsFlow().filter { it.barId == deviceId }
            .flatMapConcat { list ->
                list.values.toList().asFlow()
            }.map {
                TransportMetaInfoData.Pair(it.first, it.second)
            }
    }
}