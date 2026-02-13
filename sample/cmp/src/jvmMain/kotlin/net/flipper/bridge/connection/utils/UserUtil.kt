package net.flipper.bridge.connection.utils

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.core.ktor.getHttpClient
import kotlin.uuid.Uuid

@Serializable
private data class AuthMeResponse(
    @SerialName("success")
    val success: AuthMeUser
)

@Serializable
private data class AuthMeUser(
    @SerialName("uid")
    val uid: Uuid,
    @SerialName("email")
    val email: String
)

suspend fun getUserPrincipal(hostApi: BUSYLibHostApi): BUSYLibUserPrincipal.Full {
    val httpClient = getHttpClient()
    val response = httpClient.get("https://${hostApi.getHost().value}/api/v0/auth/me") {
        header("Authorization", "Bearer ${Secrets.AUTH_TOKEN}")
    }
    val authMe = response.body<AuthMeResponse>()
    return BUSYLibUserPrincipal.Full(
        token = Secrets.AUTH_TOKEN,
        email = authMe.success.email,
        userId = authMe.success.uid
    )
}