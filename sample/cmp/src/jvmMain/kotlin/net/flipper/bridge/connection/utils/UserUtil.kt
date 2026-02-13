package net.flipper.bridge.connection.utils

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.core.ktor.getHttpClient

suspend fun getUserPrincipal(hostApi: BUSYLibHostApi): BUSYLibUserPrincipal.Full {
    val httpClient = getHttpClient()
    val response = httpClient.get("https://${hostApi.getHost().value}/api/v0/auth/me") {
        header("Authorization", "Bearer ${Secrets.AUTH_TOKEN}")
    }
    return response.body<BUSYLibUserPrincipal.Full>()
}