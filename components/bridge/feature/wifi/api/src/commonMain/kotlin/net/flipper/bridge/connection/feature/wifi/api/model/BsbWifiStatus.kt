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
    data class Connecting(val ssid: String?) : BsbWifiStatus
    data class Disconnecting(val ssid: String?) : BsbWifiStatus
    data class Reconnecting(val ssid: String?) : BsbWifiStatus
}
