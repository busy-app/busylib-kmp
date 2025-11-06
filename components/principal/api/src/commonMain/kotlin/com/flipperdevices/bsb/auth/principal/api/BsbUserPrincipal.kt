package com.flipperdevices.bsb.auth.principal.api

sealed interface BsbUserPrincipal {

    sealed interface Token : BsbUserPrincipal {
        val token: String

        data class Impl(override val token: String) : Token

        companion object {
            operator fun invoke(token: String): Token = Impl(token)
        }
    }

    data class Full(
        override val token: String,
        val email: String,
        val userId: String?
    ) : Token

    data object Empty : BsbUserPrincipal

    data object Loading : BsbUserPrincipal
}
