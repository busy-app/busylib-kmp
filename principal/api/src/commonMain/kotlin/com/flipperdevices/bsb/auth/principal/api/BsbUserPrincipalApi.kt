package com.flipperdevices.bsb.auth.principal.api

import kotlinx.coroutines.flow.StateFlow

interface BsbUserPrincipalApi {
    fun getPrincipalFlow(): StateFlow<BsbUserPrincipal>

    suspend fun <T> withTokenPrincipal(block: suspend (BsbUserPrincipal.Token) -> Result<T>): Result<T>

    fun setPrincipal(principal: BsbUserPrincipal)
}
