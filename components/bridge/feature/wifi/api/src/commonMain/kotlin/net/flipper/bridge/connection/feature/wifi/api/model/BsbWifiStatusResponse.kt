package net.flipper.bridge.connection.feature.wifi.api.model

data class BsbWifiStatusResponse(
    val state: BsbWifiState,
    val ssid: String? = null,
    val bssid: String? = null,
    val channel: Int? = null,
    val rssi: Int? = null,
    val security: BsbWifiSecurityMethod? = null,
    val ipConfig: BsbWifiIpConfig? = null
) {
    data class BsbWifiIpConfig(
        val ipMethod: BsbWifiIpMethod? = null,
        val ipType: BsbWifiIpType? = null,
        val address: String? = null
    )

    enum class BsbWifiState {
        UNKNOWN,
        DISCONNECTED,
        CONNECTED,
        CONNECTING,
        DISCONNECTING,
        RECONNECTING
    }
}
