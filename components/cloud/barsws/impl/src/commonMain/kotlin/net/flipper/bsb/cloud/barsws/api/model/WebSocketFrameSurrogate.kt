package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WebSocketFrameSurrogate(
    @SerialName("type")
    val type: String? = null,
    @SerialName("error")
    val error: String? = null
)
