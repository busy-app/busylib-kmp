package net.flipper.core.ktor.util

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal

fun HttpRequestBuilder.addAuthHeader(principal: BUSYLibUserPrincipal.Token) {
    headers[HttpHeaders.Authorization] = "Bearer ${principal.token}"
}
