package net.flipper.bridge.connection.device.bsb.impl.utils

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import net.flipper.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import net.flipper.bridge.connection.feature.ble.api.FBleFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.settings.api.FSettingsFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import kotlin.reflect.KClass

object FZeroFeatureClassToEnumMapper {
    private val classToEnumMap: ImmutableMap<KClass<out FDeviceFeatureApi>, FDeviceFeature> =
        FDeviceFeature.entries.associateBy { featureEnumToClass(it) }.toPersistentMap()

    @Suppress("CyclomaticComplexMethod")
    private fun featureEnumToClass(feature: FDeviceFeature): KClass<out FDeviceFeatureApi> {
        return when (feature) {
            FDeviceFeature.RPC_EXPOSED -> FRpcFeatureApi::class
            FDeviceFeature.DEVICE_INFO -> FDeviceInfoFeatureApi::class
            FDeviceFeature.BATTERY_INFO -> FDeviceBatteryInfoFeatureApi::class
            FDeviceFeature.WIFI -> FWiFiFeatureApi::class
            FDeviceFeature.BLE -> FBleFeatureApi::class
            FDeviceFeature.SCREEN_STREAMING -> FScreenStreamingFeatureApi::class
            FDeviceFeature.FIRMWARE_UPDATE -> FFirmwareUpdateFeatureApi::class
            FDeviceFeature.RPC_CRITICAL -> FRpcCriticalFeatureApi::class
            FDeviceFeature.LINKED_USER_STATUS -> FLinkedInfoOnDemandFeatureApi::class
            FDeviceFeature.EVENTS -> FEventsFeatureApi::class
            FDeviceFeature.SETTINGS -> FSettingsFeatureApi::class
            FDeviceFeature.ON_CALL -> FOnCallFeatureApi::class
        }
    }

    fun get(clazz: KClass<out FDeviceFeatureApi>): FDeviceFeature? {
        return classToEnumMap[clazz]
    }
}
