package com.flipperdevices.bridge.connection.feature.common.api

import dev.zacsweers.metro.MapKey

enum class FDeviceFeature {
    RPC_EXPOSED,
    RPC_CRITICAL,
    DEVICE_INFO,
    BATTERY_INFO,
    WIFI,
    SCREEN_STREAMING,
    FIRMWARE_UPDATE,
    LINKED_USER_STATUS
}

@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class FDeviceFeatureQualifier(val enum: FDeviceFeature)
