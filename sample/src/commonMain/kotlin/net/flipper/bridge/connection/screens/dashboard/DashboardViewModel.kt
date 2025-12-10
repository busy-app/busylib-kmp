package net.flipper.bridge.connection.screens.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.core.busylib.ktx.common.FlipperDispatchers

class DashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DecomposeViewModel() {
    private val deviceNameFlow = featureProvider
        .get(FDeviceInfoFeatureApi::class)
        .flatMapLatest { feature ->
            when (feature) {
                FFeatureStatus.NotFound,
                FFeatureStatus.Retrieving,
                FFeatureStatus.Unsupported -> MutableStateFlow(null)

                is FFeatureStatus.Supported<FDeviceInfoFeatureApi> ->
                    feature
                        .featureApi
                        .getDeviceName()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun getDeviceName(): StateFlow<String?> = deviceNameFlow

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
