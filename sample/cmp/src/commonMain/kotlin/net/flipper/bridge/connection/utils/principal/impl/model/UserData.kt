package net.flipper.bridge.connection.utils.principal.impl.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class UserData(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("expires_in")
    val expiresIn: Instant,
    @SerialName("email")
    val email: String,
    @SerialName("user_id")
    val userId: Uuid
)
