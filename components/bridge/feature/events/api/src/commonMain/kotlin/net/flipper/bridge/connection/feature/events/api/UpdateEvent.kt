package net.flipper.bridge.connection.feature.events.api

enum class UpdateEvent(val bitIndex: Int? = null, val webSocketKey: String? = null) {
    DEVICE_NAME(bitIndex = 0, webSocketKey = "name"),
    BRIGHTNESS(webSocketKey = "front_brightness"),
    BLE_STATUS,
    AUDIO_VOLUME(webSocketKey = "volume"),
    WIFI_STATUS,
    UPDATER_UPDATE_STATUS(webSocketKey = "update_available_version"),
    SMART_HOME_STATUS_CHANGED,
    TIMESTAMP_CHANGED,
    TIMEZONE_CHANGED,
    BATTERY_CHARGE(webSocketKey = "battery_charge"),
    POWER_STATE(webSocketKey = "power_state"),
    RSSI(webSocketKey = "rssi")
}
