@file:Suppress("TooManyFunctions")

package net.flipper.busylib

import net.flipper.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import net.flipper.bridge.connection.feature.ble.api.FBleFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.settings.api.FSettingsFeatureApi
import net.flipper.bridge.connection.feature.smarthome.api.FSmartHomeFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap

// Battery Info Feature
fun FFeatureProvider.getBatteryInfoFeature(): WrappedFlow<FFeatureStatus<FDeviceBatteryInfoFeatureApi>> {
    return get(FDeviceBatteryInfoFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getBatteryInfoFeatureSync(): FDeviceBatteryInfoFeatureApi? {
    return this.getSync(FDeviceBatteryInfoFeatureApi::class)
}

// WiFi Feature
fun FFeatureProvider.getWiFiFeature(): WrappedFlow<FFeatureStatus<FWiFiFeatureApi>> {
    return get(FWiFiFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getWiFiFeatureSync(): FWiFiFeatureApi? {
    return this.getSync(FWiFiFeatureApi::class)
}

// Linked Info On Demand Feature
fun FFeatureProvider.getFLinkedInfoOnDemandFeature(): WrappedFlow<FFeatureStatus<FLinkedInfoOnDemandFeatureApi>> {
    return get(FLinkedInfoOnDemandFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getFLinkedInfoOnDemandFeatureSync(): FLinkedInfoOnDemandFeatureApi? {
    return this.getSync(FLinkedInfoOnDemandFeatureApi::class)
}

// OnCall Feature
fun FFeatureProvider.getOnCallFeature(): WrappedFlow<FFeatureStatus<FOnCallFeatureApi>> {
    return get(FOnCallFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getOnCallFeatureSync(): FOnCallFeatureApi? {
    return this.getSync(FOnCallFeatureApi::class)
}

// Settings Feature
fun FFeatureProvider.getSettingsFeature(): WrappedFlow<FFeatureStatus<FSettingsFeatureApi>> {
    return this.get(FSettingsFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getSettingsFeatureSync(): FSettingsFeatureApi? {
    return this.getSync(FSettingsFeatureApi::class)
}

// BLE Feature
fun FFeatureProvider.getBleFeature(): WrappedFlow<FFeatureStatus<FBleFeatureApi>> {
    return get(FBleFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getBleFeatureSync(): FBleFeatureApi? {
    return this.getSync(FBleFeatureApi::class)
}

// Device Info Feature
fun FFeatureProvider.getDeviceInfoFeature(): WrappedFlow<FFeatureStatus<FDeviceInfoFeatureApi>> {
    return get(FDeviceInfoFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getDeviceInfoFeatureSync(): FDeviceInfoFeatureApi? {
    return this.getSync(FDeviceInfoFeatureApi::class)
}

// Firmware Update Feature
fun FFeatureProvider.getFirmwareUpdateFeature(): WrappedFlow<FFeatureStatus<FFirmwareUpdateFeatureApi>> {
    return get(FFirmwareUpdateFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getFirmwareUpdateFeatureSync(): FFirmwareUpdateFeatureApi? {
    return this.getSync(FFirmwareUpdateFeatureApi::class)
}

// Screen Streaming Feature
fun FFeatureProvider.getScreenStreamingFeature(): WrappedFlow<FFeatureStatus<FScreenStreamingFeatureApi>> {
    return get(FScreenStreamingFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getScreenStreamingFeatureSync(): FScreenStreamingFeatureApi? {
    return this.getSync(FScreenStreamingFeatureApi::class)
}

// Smart Home Feature
fun FFeatureProvider.getSmartHomeFeature(): WrappedFlow<FFeatureStatus<FSmartHomeFeatureApi>> {
    return get(FSmartHomeFeatureApi::class).wrap()
}

suspend fun FFeatureProvider.getSmartHomeFeatureSync(): FSmartHomeFeatureApi? {
    return this.getSync(FSmartHomeFeatureApi::class)
}
