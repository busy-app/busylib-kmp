package net.flipper.bsb.auth.principal.api

import kotlin.uuid.Uuid

class BUSYLibUserPrincipalToken(
    override val userId: Uuid,
    private val tokenProvider: suspend (failedToken: String?) -> String
) : BUSYLibUserPrincipal.Token {
    override suspend fun getToken(failedToken: String?): String = tokenProvider(failedToken)
}
