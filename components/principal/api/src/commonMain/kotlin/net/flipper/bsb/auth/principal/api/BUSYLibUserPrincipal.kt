package net.flipper.bsb.auth.principal.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed interface BUSYLibUserPrincipal {

    @Serializable
    sealed interface Token : BUSYLibUserPrincipal {
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
        val userId: Uuid?
    ) : Token

    @Serializable
    data object Empty : BUSYLibUserPrincipal

    @Serializable
    data object Loading : BUSYLibUserPrincipal
}
