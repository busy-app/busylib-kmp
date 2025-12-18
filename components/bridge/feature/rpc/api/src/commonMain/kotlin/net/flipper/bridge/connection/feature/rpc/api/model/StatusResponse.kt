package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    @SerialName("state")
    val state: State,

    @SerialName("ssid")
    val ssid: String? = null,

    @SerialName("bssid")
    val bssid: String? = null,

    @SerialName("channel")
    val channel: Int? = null,

    @SerialName("rssi")
    val rssi: Int? = null,

    @SerialName("security")
    val security: WifiSecurityMethod? = null,

    @SerialName("ip_config")
    val ipConfig: IpConfig? = null
) {
    @Serializable
    data class IpConfig(
        @SerialName("ip_method")
        val ipMethod: WifiIpMethod? = null,

        @SerialName("ip_type")
        val ipType: WifiIpType? = null,

        @SerialName("address")
        val address: String? = null
    )

    @Serializable
    enum class State {
        @SerialName("unknown")
        UNKNOWN,

        @SerialName("disconnected")
        DISCONNECTED,

        @SerialName("connected")
        CONNECTED,

        @SerialName("connecting")
        CONNECTING,

        @SerialName("disconnecting")
        DISCONNECTING,

        @SerialName("reconnecting")
        RECONNECTING
    }
}
