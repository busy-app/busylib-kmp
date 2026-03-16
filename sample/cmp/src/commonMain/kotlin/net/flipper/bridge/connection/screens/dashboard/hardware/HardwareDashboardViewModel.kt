package net.flipper.bridge.connection.screens.dashboard.hardware

import kotlinx.coroutines.flow.flow
import net.flipper.bridge.connection.feature.about.api.FAboutFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel

class HardwareDashboardViewModel(
    featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    val aboutDeviceFlow = featureProvider
        .get(FAboutFeatureApi::class)
        .getResource { flow { emit(it.getAboutDevice().getOrNull()) } }
}
