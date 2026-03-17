package net.flipper.bridge.connection.utils.principal.impl.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
internal data class AuthSignInRequest(
    @SerialName("username")
    val username: String,
    @SerialName("password")
    val password: String,
    @SerialName("code_challenge")
    val codeChallenge: String
)

@Serializable
internal data class AuthSignInResponse(
    @SerialName("success")
    val success: AuthCodeData
)

@Serializable
internal data class AuthCodeData(
    @SerialName("auth_code")
    val authCode: String
)

@Serializable
internal data class AuthExchangeRequest(
    @SerialName("auth_code")
    val authCode: String,
    @SerialName("code_verifier")
    val codeVerifier: String,
    @SerialName("platform")
    val platform: String = "native"
)

@Serializable
internal data class AuthExchangeResponse(
    @SerialName("success")
    val success: AuthTokensData
)

@Serializable
internal data class AuthTokensData(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("expires_in")
    val expiresInSec: Long
)

@Serializable
internal data class AuthRefreshRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

@Serializable
internal data class AuthRefreshResponse(
    @SerialName("success")
    val success: AuthTokensData
)

@Serializable
internal data class AuthMeResponse(
    @SerialName("success")
    val success: AuthMeUser
)

@Serializable
internal data class AuthMeUser(
    @SerialName("uid")
    val uid: Uuid,
    @SerialName("email")
    val email: String
)
