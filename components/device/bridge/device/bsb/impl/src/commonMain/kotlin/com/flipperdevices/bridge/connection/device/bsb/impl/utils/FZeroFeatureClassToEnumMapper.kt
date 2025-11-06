package com.flipperdevices.bridge.connection.device.bsb.impl.utils

import com.flipperdevices.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import com.flipperdevices.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import com.flipperdevices.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import com.flipperdevices.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentMap
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
            FDeviceFeature.SCREEN_STREAMING -> FScreenStreamingFeatureApi::class
            FDeviceFeature.FIRMWARE_UPDATE -> FFirmwareUpdateFeatureApi::class
            FDeviceFeature.RPC_CRITICAL -> FRpcCriticalFeatureApi::class
            FDeviceFeature.LINKED_USER_STATUS -> FLinkedInfoOnDemandFeatureApi::class
        }
    }

    fun get(clazz: KClass<out FDeviceFeatureApi>): FDeviceFeature? {
        return classToEnumMap[clazz]
    }
}
