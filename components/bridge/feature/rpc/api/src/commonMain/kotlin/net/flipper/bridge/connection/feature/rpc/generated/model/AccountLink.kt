package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountLink(
    @SerialName("code")
    val code: kotlin.String? = null,
    @SerialName("expires_at")
    val expiresAt: kotlin.Int? = null
)
