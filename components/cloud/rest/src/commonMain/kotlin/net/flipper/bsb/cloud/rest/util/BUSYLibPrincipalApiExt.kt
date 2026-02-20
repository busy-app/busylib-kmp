package net.flipper.bsb.cloud.rest.util

import kotlinx.coroutines.flow.first
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal

suspend fun BUSYLibPrincipalApi.requireTokenPrincipal(): BUSYLibUserPrincipal.Token {
    val principal = this.getPrincipalFlow().first()
    val tokenPrincipal = principal as? BUSYLibUserPrincipal.Token
    return tokenPrincipal ?: error("Token principal is null")
}
