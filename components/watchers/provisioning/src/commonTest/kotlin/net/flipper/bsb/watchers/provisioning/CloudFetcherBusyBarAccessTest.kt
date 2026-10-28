package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.BUSYBarWebSocket
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketApi
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import net.flipper.bsb.watchers.provisioning.fakes.FakeCloudBarsApi
import net.flipper.bsb.watchers.provisioning.fakes.FakeNetworkStateApi
import net.flipper.bsb.watchers.provisioning.fakes.FakePrincipalApi
import net.flipper.bsb.watchers.provisioning.fakes.cloudBar
import net.flipper.bsb.watchers.provisioning.fakes.fakeToken
import net.flipper.bsb.watchers.provisioning.utils.CloudFetcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class CloudFetcherBusyBarAccessTest {

    @Test
    fun GIVEN_bars_list_WHEN_access_event_arrives_THEN_bars_list_is_not_emitted_again() = runTest {
        val existingBar = cloudBar(id = Uuid.random(), label = "Existing")
        val linkedBar = BUSYBarWebSocket(cloudId = Uuid.random(), hardwareId = "hw-linked", name = "Linked")
        val webSocketEvents = MutableSharedFlow<WebSocketEvent>()
        val webSocket = object : BSBWebSocket {
            override fun getEventsFlow(): Flow<WebSocketEvent> = webSocketEvents
        }
        val cloudFetcher = CloudFetcher(
            principalApi = FakePrincipalApi(MutableStateFlow<BUSYLibUserPrincipal>(fakeToken())),
            networkStateApi = FakeNetworkStateApi(MutableStateFlow(true)),
            busyCloudBarsApi = FakeCloudBarsApi(Result.success(listOf(existingBar))),
            wsApi = object : CloudWebSocketApi {
                override fun getWSFlow(): Flow<BSBWebSocket?> = flowOf(webSocket)
            }
        )
        val emissions = mutableListOf<List<BusyCloudBar>>()

        val scope = CoroutineScope(
            SupervisorJob(backgroundScope.coroutineContext.job) + StandardTestDispatcher(testScheduler)
        )

        scope.launch { cloudFetcher.getBarsFlow().collect { emissions.add(it) } }
        advanceUntilIdle()
        webSocketEvents.emit(WebSocketEvent.BusyBarAccessUpdatedEvent(granted = true, graceUntil = null))
        advanceUntilIdle()

        assertEquals(listOf(listOf(existingBar)), emissions)

        webSocketEvents.emit(WebSocketEvent.LinkEvent(linkedBar))
        advanceUntilIdle()

        assertEquals(
            listOf(
                listOf(existingBar),
                listOf(
                    existingBar,
                    BusyCloudBar(id = linkedBar.cloudId, hardwareId = linkedBar.hardwareId, label = linkedBar.name)
                )
            ),
            emissions
        )
    }
}
