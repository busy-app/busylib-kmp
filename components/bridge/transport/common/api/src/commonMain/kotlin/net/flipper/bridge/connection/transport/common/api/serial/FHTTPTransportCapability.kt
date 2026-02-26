package net.flipper.bridge.connection.transport.common.api.serial

/**
 * Some requests can be executed only by specific transport. For example:
 * - WiFi connect can be executed only by BLE
 */
enum class FHTTPTransportCapability {
    BB_WEBSOCKET_SUPPORTED,
    BB_DOWNLOAD_UPDATE_SUPPORTED
}

const val HEADER_NAME_REQUEST_CAPABILITY = "X-BUSYLib-Request-Capability-Origin"
