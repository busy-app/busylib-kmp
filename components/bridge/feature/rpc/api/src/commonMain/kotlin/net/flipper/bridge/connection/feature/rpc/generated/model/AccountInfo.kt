package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.Uuid

@Serializable
data class AccountInfo(
    @SerialName("linked")
    val linked: kotlin.Boolean,
    @SerialName("id")
    val id: kotlin.String? = null,
    @SerialName("email")
    val email: kotlin.String? = null,
    @SerialName("user_id")
    val userIdRaw: kotlin.String? = null
) {

    @Transient
    val userId: Uuid? = userIdRaw?.let(Uuid::parse)
}
