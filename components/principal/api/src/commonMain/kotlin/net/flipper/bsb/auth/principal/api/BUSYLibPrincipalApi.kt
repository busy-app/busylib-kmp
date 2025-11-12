package net.flipper.bsb.auth.principal.api

import kotlinx.coroutines.flow.StateFlow

interface BUSYLibPrincipalApi {
    fun getPrincipalFlow(): StateFlow<BUSYLibUserPrincipal>
}
