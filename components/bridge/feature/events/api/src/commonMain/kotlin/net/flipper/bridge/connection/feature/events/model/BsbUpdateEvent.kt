package net.flipper.bridge.connection.feature.events.model

/**
 * Update events which goes only from BusyBar (socket/lan/cloud)
 */
@Deprecated("Use BusyLibUpdateEvent instead")
enum class BsbUpdateEvent(
    val bitIndex: Int? = null,
    val webSocketKey: String? = null
) : UpdateEvent {
    BLE_STATUS,
    UPDATER_UPDATE_STATUS(webSocketKey = "update_available_version"),
    TIMESTAMP_CHANGED,
}
