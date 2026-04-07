package net.flipper.bridge.connection.screens.dashboard.smarthome

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.smarthome.api.FSmartHomeFeatureApi
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel

class SmartHomeDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    val commissionedFabricsFlow = featureProvider
        .get(FSmartHomeFeatureApi::class)
        .getResource { it.getCommissionedFabricsFlow() }

    val pairCodeWithTimeLeftFlow = featureProvider
        .get(FSmartHomeFeatureApi::class)
        .getResource { it.getPairCodeWithTimeLeft() }

    private val mutableState = MutableStateFlow(SmartHomeDashboardState())
    val state: StateFlow<SmartHomeDashboardState> = mutableState

    fun requestPairCode() = runAction("smart_home pair code") {
        val smartHomeApi = requireFeature<FSmartHomeFeatureApi>(featureProvider, "Smart Home")
        val pairCode = smartHomeApi.getPairCode().getOrThrow()
        mutableState.value = mutableState.value.copy(
            lastPairingCode = pairCode.manualCode,
            lastPairingQr = pairCode.qrCode,
            lastPairingExpiresAt = pairCode.availableUntil.toString()
        )
        appendLog("Pair code received until ${pairCode.availableUntil}")
    }

    fun forgetAllPairings() = runAction("smart_home forget pairings") {
        val smartHomeApi = requireFeature<FSmartHomeFeatureApi>(featureProvider, "Smart Home")
        smartHomeApi.forgetAllPairings().getOrThrow()
        appendLog("All pairings removed")
    }
}

data class SmartHomeDashboardState(
    val lastPairingCode: String? = null,
    val lastPairingQr: String? = null,
    val lastPairingExpiresAt: String? = null
)
