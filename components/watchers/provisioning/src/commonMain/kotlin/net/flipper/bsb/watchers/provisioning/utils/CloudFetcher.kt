package net.flipper.bsb.watchers.provisioning.utils

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.BUSYBarWebSocket
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketApi
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

@Inject
class CloudFetcher(
    private val principalApi: BUSYLibPrincipalApi,
    private val networkStateApi: BUSYLibNetworkStateApi,
    private val busyCloudBarsApi: BusyCloudBarsApi,
    private val wsApi: CloudWebSocketApi
) : LogTagProvider {
    override val TAG = "CloudFetcher"

    fun getBarsFlow(): Flow<List<BusyCloudBar>> {
        return combine(
            principalApi.getPrincipalFlow(),
            networkStateApi.isNetworkAvailableFlow
        ) { principal, isNetworkAvailable ->
            if (isNetworkAvailable && principal is BUSYLibUserPrincipal.Token) {
                getBarsFlow(principal)
            } else {
                emptyFlow()
            }
        }.flatMapLatest { it }
    }

    private fun getBarsFlow(
        principal: BUSYLibUserPrincipal.Token,
    ): Flow<List<BusyCloudBar>> = flow {
        val bars = busyCloudBarsApi.getBarsList(principal).onFailure {
            error(it) { "Failed to get bars list" }
        }.getOrNull() ?: return@flow

        info { "Got bars list: $bars" }
        emit(bars)
        val barsMap = bars.associateBy { it.id }.toMutableMap()

        wsApi.getWSFlow().flatMapLatest {
            it?.getEventsFlow() ?: emptyFlow()
        }.collect {
            info { "Received event: $it" }
            when (it) {
                is WebSocketEvent.LinkEvent -> {
                    barsMap[it.device.cloudId] = it.device.toBusyCloudBar()
                }

                is WebSocketEvent.NameChangeEvent -> {
                    barsMap[it.device.cloudId] = it.device.toBusyCloudBar()
                }

                is WebSocketEvent.UnlinkEvent -> {
                    barsMap.remove(it.device.cloudId)
                }
            }
            val barsList = barsMap.values.toList()
            info { "New bars list: $barsList" }
            emit(barsList)
        }
    }
}

private fun BUSYBarWebSocket.toBusyCloudBar() = BusyCloudBar(
    id = cloudId,
    hardwareId = hardwareId,
    label = name
)