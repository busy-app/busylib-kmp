package net.flipper.bsb.auth.principal.api

import kotlinx.coroutines.flow.StateFlow

interface BsbUserPrincipalApi {
    fun getPrincipalFlow(): StateFlow<BsbUserPrincipal>
}
