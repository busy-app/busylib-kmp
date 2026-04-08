package net.flipper.tools.multistream.impl

import me.tatarka.inject.annotations.Assisted
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketBarsApi
import kotlin.uuid.Uuid

class MultiStreamStatusApi(
    private val deviceId: Uuid,
    private val webSocketBarsApi: CloudWebSocketBarsApi,
) : FStatusStreamingApi {
}