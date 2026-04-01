package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RpcTimezoneInfo(
    @SerialName("name")
    val timezone: String,
    val offset: String? = null,
    val abbr: String? = null
)
