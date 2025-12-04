package net.flipper.bridge.connection.screens.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel

class DashboardViewModel(
    featureProvider: FFeatureProvider
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
}
