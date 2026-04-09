package net.flipper.bridge.connection.utils.principal.impl

import net.flipper.bridge.connection.utils.principal.impl.token.AuthTokenProvider
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import kotlin.uuid.Uuid

class UserPrincipalImpl(
    private val tokenProvider: AuthTokenProvider,
    val email: String,
    override val userId: Uuid
) : BUSYLibUserPrincipal.Token {

    override suspend fun getToken(failedToken: String?): String {
        return tokenProvider.getToken(failedToken)
    }
}
