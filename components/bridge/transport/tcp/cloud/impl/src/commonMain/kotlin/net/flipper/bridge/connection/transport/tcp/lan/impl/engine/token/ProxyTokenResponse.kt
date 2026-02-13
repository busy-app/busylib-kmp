package net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProxyTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresInSec: Int
)
