package net.flipper.bridge.connection.feature.events.model

/**
 * Update events which goes only from BusyBar (socket/lan/cloud)
 */
enum class BsbUpdateEvent(
    val bitIndex: Int? = null,
    val webSocketKey: String? = null
) : UpdateEvent {
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
