package net.flipper.busylib.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bsb.auth.principal.api.BsbUserPrincipal
import net.flipper.bsb.auth.principal.api.BsbUserPrincipalApi

class BUSYLibPrincipalApiImpl : BsbUserPrincipalApi {
    private val userStateFlow = MutableStateFlow<BsbUserPrincipal>(BsbUserPrincipal.Loading)

    override fun getPrincipalFlow(): StateFlow<BsbUserPrincipal> {
        return userStateFlow
    }

    suspend fun update(userPrincipal: BsbUserPrincipal) {
        userStateFlow.emit(userPrincipal)
    }
}