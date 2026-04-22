package net.flipper.bsb.cloud.barsws.api.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocketInternal
import net.flipper.bsb.cloud.barsws.api.utils.CloudWebSocketApiInternal

internal class MockCloudWebSocketApi(
    initialWs: BSBWebSocketInternal? = null
) : CloudWebSocketApiInternal {
    private val _wsFlow = MutableStateFlow<BSBWebSocketInternal?>(initialWs)

    override fun getWSFlow(): Flow<BSBWebSocket?> = _wsFlow

    override fun getWSInternalFlow(): Flow<BSBWebSocketInternal?> = _wsFlow

    fun setWebSocket(ws: BSBWebSocketInternal?) {
        _wsFlow.value = ws
    }
}
