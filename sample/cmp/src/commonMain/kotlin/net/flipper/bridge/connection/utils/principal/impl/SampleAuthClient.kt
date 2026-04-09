package net.flipper.bridge.connection.utils.principal.impl

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import net.flipper.bridge.connection.utils.principal.impl.model.AuthExchangeRequest
import net.flipper.bridge.connection.utils.principal.impl.model.AuthExchangeResponse
import net.flipper.bridge.connection.utils.principal.impl.model.AuthMeResponse
import net.flipper.bridge.connection.utils.principal.impl.model.AuthMeUser
import net.flipper.bridge.connection.utils.principal.impl.model.AuthRefreshRequest
import net.flipper.bridge.connection.utils.principal.impl.model.AuthRefreshResponse
import net.flipper.bridge.connection.utils.principal.impl.model.AuthSignInRequest
import net.flipper.bridge.connection.utils.principal.impl.model.AuthSignInResponse
import net.flipper.bridge.connection.utils.principal.impl.model.AuthTokensData
import net.flipper.core.ktor.getHttpClient

internal object SampleAuthClient {
    private val httpClient = getHttpClient()

    suspend fun signIn(
        host: String,
        email: String,
        password: String
    ): AuthTokensData {
        val codeVerifier = PKCEHelper.generateCodeVerifier()
        val codeChallenge = PKCEHelper.generateCodeChallenge(codeVerifier)

        val signInResponse = httpClient.post("https://$host/api/auth/v1/tokens") {
            contentType(ContentType.Application.Json)
            header("X-Refresh-Token-Transport", "json")
            setBody(
                AuthSignInRequest(
                    username = email,
                    password = password,
                    codeChallenge = codeChallenge
                )
            )
        }.body<AuthSignInResponse>()

        val exchangeResponse = httpClient.post("https://$host/api/auth/v1/tokens/exchange") {
            contentType(ContentType.Application.Json)
            header("X-Refresh-Token-Transport", "json")
            setBody(
                AuthExchangeRequest(
                    authCode = signInResponse.success.authCode,
                    codeVerifier = codeVerifier,
                    platform = "mobile"
                )
            )
        }.body<AuthExchangeResponse>()

        return exchangeResponse.success
    }

    suspend fun refreshTokens(
        host: String,
        refreshToken: String
    ): AuthTokensData {
        val response = httpClient.post("https://$host/api/auth/v1/tokens/refresh") {
            contentType(ContentType.Application.Json)
            header("X-Refresh-Token-Transport", "json")
            setBody(AuthRefreshRequest(refreshToken = refreshToken))
        }.body<AuthRefreshResponse>()
        return response.success
    }

    suspend fun getMe(
        host: String,
        accessToken: String
    ): AuthMeUser {
        val response = httpClient.get("https://$host/api/auth/v1/me") {
            header("Authorization", "Bearer $accessToken")
        }.body<AuthMeResponse>()
        return response.success
    }

    suspend fun logout(
        host: String,
        accessToken: String
    ) {
        httpClient.delete("https://$host/api/auth/v1/sessions/current") {
            header("Authorization", "Bearer $accessToken")
        }
    }
}
