package net.flipper.bridge.connection.screens.dashboard.overview

import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.settings.api.FSettingsFeatureApi
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel

class OverviewDashboardViewModel(
    featureProvider: FFeatureProvider
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

    val deviceVersionFlow = featureProvider
        .get(FDeviceInfoFeatureApi::class)
        .getResource { it.deviceVersionFlow }
}
