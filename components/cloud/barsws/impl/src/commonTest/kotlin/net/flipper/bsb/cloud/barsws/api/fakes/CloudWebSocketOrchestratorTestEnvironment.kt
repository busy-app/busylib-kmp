package net.flipper.bsb.cloud.barsws.api.fakes

import kotlinx.coroutines.CoroutineScope
import net.flipper.bsb.cloud.barsws.api.orchestrator.CloudWebSocketOrchestratorApiImpl

internal class CloudWebSocketOrchestratorTestEnvironment(
    scope: CoroutineScope,
    initiallyConnected: Boolean = true
) {
    val mockWs = MockBSBWebSocket()
    val mockApi = MockCloudWebSocketApi(
        initialWs = if (initiallyConnected) mockWs else null
    )
    val orchestrator = CloudWebSocketOrchestratorApiImpl(mockApi, scope)
}
