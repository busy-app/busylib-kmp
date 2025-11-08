package com.flipperdevices.bridge.connection.feature.common.api

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
annotation class FDeviceFeatureQualifier(val enum: FDeviceFeature)
