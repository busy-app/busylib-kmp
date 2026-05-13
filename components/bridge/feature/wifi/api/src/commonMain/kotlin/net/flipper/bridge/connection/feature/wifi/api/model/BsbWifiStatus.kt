package net.flipper.bridge.connection.feature.wifi.api.model

sealed interface BsbWifiStatus {
    data class Connected(
        val ssid: String?,
        val bssid: String?,
        val channel: Int?,
        val rssi: Int?,
        val security: BsbWifiSecurityMethod?,
        val ipConfig: BsbWifiIpConfig
    ) : BsbWifiStatus {
        data class BsbWifiIpConfig(
            val address: String?,
            val ipMethod: BsbWifiIpMethod?,
            val ipType: BsbWifiIpType?
        )
    }

    data object Unknown : BsbWifiStatus
    data object Disconnected : BsbWifiStatus
    data object Connecting : BsbWifiStatus
    data object Disconnecting : BsbWifiStatus
    data object Reconnecting : BsbWifiStatus
}
