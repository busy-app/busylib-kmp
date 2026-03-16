package net.flipper.bridge.connection.screens.dashboard.screenstreaming

import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel

class ScreenStreamingDashboardViewModel(
    featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    val screenStreamingImagesFlow = featureProvider
        .get(FScreenStreamingFeatureApi::class)
        .getResource { it.busyImageFormatFlow }
}
