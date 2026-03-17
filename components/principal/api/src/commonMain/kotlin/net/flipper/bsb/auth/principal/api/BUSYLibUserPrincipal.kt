package net.flipper.bsb.auth.principal.api

import kotlin.uuid.Uuid

sealed interface BUSYLibUserPrincipal {

    interface Token : BUSYLibUserPrincipal {
        val userId: Uuid

        suspend fun getToken(failedToken: String?): String
    }

    data object Empty : BUSYLibUserPrincipal

    data object Loading : BUSYLibUserPrincipal
}
