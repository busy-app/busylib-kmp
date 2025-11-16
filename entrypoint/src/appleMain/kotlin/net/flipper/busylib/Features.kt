package net.flipper.busylib

import net.flipper.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap

fun FFeatureProvider.getBatteryInfoFeature(): WrappedFlow<FFeatureStatus<FDeviceBatteryInfoFeatureApi>> {
    return get(FDeviceBatteryInfoFeatureApi::class).wrap()
}

fun FFeatureProvider.getWiFiFeature(): WrappedFlow<FFeatureStatus<FWiFiFeatureApi>> {
    return get(FWiFiFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getWiFiFeatureSync(): FWiFiFeatureApi? {
    return this.getSync(FWiFiFeatureApi::class)
}
