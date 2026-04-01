package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.Serializable

@Serializable
data class RpcTimezoneInfo(
    val name: String,
    val offset: String,
    val abbr: String
)
