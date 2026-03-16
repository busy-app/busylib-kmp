package net.flipper.bridge.connection.screens.dashboard.oncall

import kotlinx.coroutines.launch
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel
import net.flipper.core.busylib.ktx.common.FlipperDispatchers

class OnCallDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    fun startOnCall() {
        viewModelScope.launch(FlipperDispatchers.default) {
            featureProvider.getSync(FOnCallFeatureApi::class)?.start()
        }
    }

    fun stopOnCall() {
        viewModelScope.launch(FlipperDispatchers.default) {
            featureProvider.getSync(FOnCallFeatureApi::class)?.stop()
        }
    }
}
