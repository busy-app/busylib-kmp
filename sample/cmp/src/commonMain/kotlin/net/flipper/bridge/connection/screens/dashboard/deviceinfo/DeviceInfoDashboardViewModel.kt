package net.flipper.bridge.connection.screens.dashboard.deviceinfo

import kotlinx.coroutines.flow.flow
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel

class DeviceInfoDashboardViewModel(
    featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    val deviceInfoFlow = featureProvider
        .get(FDeviceInfoFeatureApi::class)
        .getResource { flow { emit(it.getDeviceInfo().getOrNull()) } }

    val deviceFirmwareFlow = featureProvider
        .get(FDeviceInfoFeatureApi::class)
        .getResource { flow { emit(it.getDeviceFirmware().getOrNull()) } }

    val deviceVersionFlow = featureProvider
        .get(FDeviceInfoFeatureApi::class)
        .getResource { it.deviceVersionFlow }
}
