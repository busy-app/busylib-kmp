package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class RpcLinkedAccountInfo(
    @SerialName("linked")
    val linked: Boolean,
    @SerialName("email")
    val email: String? = null,
    @SerialName("user_id")
    val userStr: String? = null
) {
    val userId: Uuid? = userStr?.let { Uuid.parse(it) }
}
