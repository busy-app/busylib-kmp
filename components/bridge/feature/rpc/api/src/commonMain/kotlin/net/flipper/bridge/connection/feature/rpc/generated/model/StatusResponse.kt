package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    @SerialName("state")
    val state: State,
    @SerialName("ssid")
    val ssid: kotlin.String? = null,
    @SerialName("bssid")
    val bssid: kotlin.String? = null,
    @SerialName("channel")
    val channel: kotlin.Int? = null,
    @SerialName("rssi")
    val rssi: kotlin.Int? = null,
    @SerialName("security")
    val security: WifiSecurityMethod? = null,
    @SerialName("ip_config")
    val ipConfig: StatusResponseIpConfig? = null
) {

    @Serializable
    enum class State(val value: kotlin.String) {
        @SerialName("unknown")
        UNKNOWN("unknown"),

        @SerialName("disconnected")
        DISCONNECTED("disconnected"),

        @SerialName("connected")
        CONNECTED("connected"),

        @SerialName("connecting")
        CONNECTING("connecting"),

        @SerialName("disconnecting")
        DISCONNECTING("disconnecting"),

        @SerialName("reconnecting")
        RECONNECTING("reconnecting")
    }
}
