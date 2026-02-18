package net.flipper.bridge.connection.screens.dashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.settings.api.FSettingsFeatureApi
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import kotlin.collections.get

class DashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DecomposeViewModel() {
    val deviceNameFlow = featureProvider
        .get(FSettingsFeatureApi::class)
        .getResource { it.getDeviceName() }

    val screenStreamingImagesFlow = featureProvider
        .get(FScreenStreamingFeatureApi::class)
        .getResource { it.busyImageFormatFlow }

    val brightnessFlow = featureProvider
        .get(FSettingsFeatureApi::class)
        .getResource { it.getBrightnessInfoFlow() }

    val volumeFlow = featureProvider
        .get(FSettingsFeatureApi::class)
        .getResource { it.getVolumeFlow() }

    fun startOnCall() {
        viewModelScope.launch(FlipperDispatchers.default) {
            val onCallFeature = featureProvider.getSync(FOnCallFeatureApi::class)
            onCallFeature?.start()
        }
    }

    fun stopOnCall() {
        viewModelScope.launch(FlipperDispatchers.default) {
            val onCallFeature = featureProvider.getSync(FOnCallFeatureApi::class)
            onCallFeature?.stop()
        }
    }

    private fun <T : FDeviceFeatureApi, R> Flow<FFeatureStatus<T>>.getResource(block: (T) -> Flow<R>): StateFlow<R?> {
        return flatMapLatest { feature ->
            when (feature) {
                FFeatureStatus.NotFound,
                FFeatureStatus.Retrieving,
                FFeatureStatus.Unsupported -> MutableStateFlow(null)

                is FFeatureStatus.Supported<T> ->
                    block(feature.featureApi)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }
}
