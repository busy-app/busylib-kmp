package net.flipper.bridge.connection.screens.dashboard.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.settings.api.FSettingsFeatureApi
import net.flipper.bridge.connection.feature.settings.model.BsbBrightness
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel
import net.flipper.core.busylib.data.Fraction

data class SettingsDashboardState(
    val isUpdatingDeviceName: Boolean = false,
    val isUpdatingBrightness: Boolean = false,
    val isUpdatingVolume: Boolean = false
)

class SettingsDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    val deviceNameFlow = featureProvider
        .get(FSettingsFeatureApi::class)
        .getResource { it.getDeviceName() }

    val brightnessFlow = featureProvider
        .get(FSettingsFeatureApi::class)
        .getResource { it.getBrightnessInfoFlow() }

    val volumeFlow = featureProvider
        .get(FSettingsFeatureApi::class)
        .getResource { it.getVolumeFlow() }

    private val mutableState = MutableStateFlow(SettingsDashboardState())
    val state: StateFlow<SettingsDashboardState> = mutableState

    fun setDeviceName(name: String) {
        val normalizedName = name.trim()
        runSettingsAction(
            actionName = "Set device name",
            onStart = { copy(isUpdatingDeviceName = true) },
            onFinish = { copy(isUpdatingDeviceName = false) }
        ) { api ->
            api.setDeviceName(normalizedName).getOrThrow()
            appendLog("Device name updated to \"$normalizedName\"")
        }
    }

    fun setVolume(percent: Int) {
        runSettingsAction(
            actionName = "Set volume",
            onStart = { copy(isUpdatingVolume = true) },
            onFinish = { copy(isUpdatingVolume = false) }
        ) { api ->
            api.setVolume(Fraction.fromWholePercent(percent)).getOrThrow()
            appendLog("Volume updated to $percent%")
        }
    }

    fun setBrightnessAuto() {
        runSettingsAction(
            actionName = "Set brightness auto",
            onStart = { copy(isUpdatingBrightness = true) },
            onFinish = { copy(isUpdatingBrightness = false) }
        ) { api ->
            api.setBrightness(BsbBrightness.Auto).getOrThrow()
            appendLog("Brightness mode updated to auto")
        }
    }

    fun setBrightness(percent: Int) {
        runSettingsAction(
            actionName = "Set brightness",
            onStart = { copy(isUpdatingBrightness = true) },
            onFinish = { copy(isUpdatingBrightness = false) }
        ) { api ->
            api.setBrightness(BsbBrightness.Number(Fraction.fromWholePercent(percent))).getOrThrow()
            appendLog("Brightness updated to $percent%")
        }
    }

    private fun runSettingsAction(
        actionName: String,
        onStart: SettingsDashboardState.() -> SettingsDashboardState,
        onFinish: SettingsDashboardState.() -> SettingsDashboardState,
        block: suspend (FSettingsFeatureApi) -> Unit
    ) {
        runAction(actionName = actionName) {
            mutableState.update(onStart)
            try {
                block(requireFeature<FSettingsFeatureApi>(featureProvider, featureName = "Settings"))
            } finally {
                mutableState.update(onFinish)
            }
        }
    }
}
