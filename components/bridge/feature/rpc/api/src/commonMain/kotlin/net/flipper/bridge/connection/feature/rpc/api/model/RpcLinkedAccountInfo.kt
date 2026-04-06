package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.Uuid

@Serializable
data class RpcLinkedAccountInfo(
    @SerialName("linked")
    val linked: Boolean,
    @SerialName("email")
    val email: String? = null,
    @SerialName("user_id")
    val userIdRaw: String? = null,
    @SerialName("id")
    val cloudId: String? = null
) {
    @Transient
    val userId: Uuid? = userIdRaw?.let { Uuid.parse(it) }
}
