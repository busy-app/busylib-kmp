package net.flipper.bridge.connection.transport.common.api.serial

/**
 * Some requests can be executed only by specific transport. For example:
 * - WiFi connect can be executed only by BLE
 */
enum class FHTTPTransportCapability {
    /**
     * Capability indicates transport is capable of using websocket connection
     */
    BB_WEBSOCKET_SUPPORTED,

    /**
     * Download update onto application and upload it on BusyBar
     */
    BB_DOWNLOAD_UPDATE_SUPPORTED,

    /**
     * Capability means a local connection is currently active
     *
     * ### Example:
     * We can disconnect Wi-Fi only if there is another local connection available (such as LAN or BLE)
     * Otherwise, if the device is connected only via the cloud,
     * disconnecting Wi-Fi would result in losing the connection to BusyBar
     */
    BB_LOCAL_CONNECTION
}

const val HEADER_NAME_REQUEST_CAPABILITY = "X-BUSYLib-Request-Capability-Origin"
