package com.flipperdevices.bsb.auth.principal.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface BsbUserPrincipal {

    @Serializable
    sealed interface Token : BsbUserPrincipal {
        val token: String

        @Serializable
        data class Impl(override val token: String) : Token

        companion object {
            operator fun invoke(token: String): Token = Impl(token)
        }
    }

    @Serializable
    data class Full(
        override val token: String,
        val email: String,
        val userId: String?
    ) : Token

    @Serializable
    data object Empty : BsbUserPrincipal

    @Serializable
    data object Loading : BsbUserPrincipal
}
