package net.flipper.bridge.connection.screens.dashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.bridge.connection.feature.settings.api.FSettingsFeatureApi
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.asFlow

class DashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DecomposeViewModel() {
    private val deviceNameFlow = featureProvider
        .get(FSettingsFeatureApi::class)
        .flatMapLatest { feature ->
            when (feature) {
                FFeatureStatus.NotFound,
                FFeatureStatus.Retrieving,
                FFeatureStatus.Unsupported -> MutableStateFlow(null)

                is FFeatureStatus.Supported<FSettingsFeatureApi> ->
                    feature
                        .featureApi
                        .getDeviceName()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val screenStreamingImagesFlow = featureProvider
        .get(FScreenStreamingFeatureApi::class)
        .flatMapLatest { feature ->
            when (feature) {
                FFeatureStatus.NotFound,
                FFeatureStatus.Retrieving,
                FFeatureStatus.Unsupported -> MutableStateFlow(null)

                is FFeatureStatus.Supported<FScreenStreamingFeatureApi> ->
                    feature
                        .featureApi
                        .busyImageFormatFlow
            }
        }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(0), replay = 1)

    fun getDeviceName(): StateFlow<String?> = deviceNameFlow
    fun getScreenStreamingImages(): Flow<BusyImageFormat?> = screenStreamingImagesFlow.asFlow()

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
}
