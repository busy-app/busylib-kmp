@file:OptIn(ExperimentalStdlibApi::class)

package net.flipper.bridge.connection.screens.device.viewmodel

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

@OptIn(ExperimentalCoroutinesApi::class)
class PingViewModel(
    private val featureProvider: FFeatureProvider,
    orchestrator: FDeviceOrchestrator
) : DecomposeViewModel(), LogTagProvider {
    override val TAG = "PingViewModel"

    private val logLines = MutableStateFlow(persistentListOf("Init PingViewModel"))

    init {
        orchestrator.getState().onEach {
            log("Device connect status is: ${it::class.simpleName}")
        }.launchIn(viewModelScope)

        featureProvider.get<FRpcFeatureApi>()
            .onEach { featureStatusSupport ->
                log("FDeviceBatteryInfoFeatureApiapi status support ${featureStatusSupport::class.simpleName}")
                if (featureStatusSupport !is FFeatureStatus.Supported) {
                    log("Device api don't support FDeviceBatteryInfoFeatureApi, so skip subscribe on bytes")
                } else {
                    log("Subscribe to receive bytes flow")
                }
            }.launchIn(viewModelScope)
    }

    fun getLogLinesState() = logLines.asStateFlow()

    fun sendPing() = viewModelScope.launch {
//        log("Request send wifi request")
//        val requestApi = featureProvider.getSync<FRpcFeatureApi>()
//        info { "Receive requestApi: $requestApi" }
//        if (requestApi == null) {
//            log("Failed receive request api")
//        } else {
//            requestApi.getWifiNetworks().onSuccess {
//                log("Response wifi request successful $it")
//            }.onFailure {
//                error(it) { "Failed to receive wifi request" }
//                log("Failed receive wifi request")
//            }
//        }
    }

    private fun log(text: String) {
        info { text }
        logLines.update { it.add(text) }
    }
}
