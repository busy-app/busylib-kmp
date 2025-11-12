package net.flipper.busylib.di

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.auth.principal.api.BsbUserPrincipal
import net.flipper.bsb.auth.principal.api.BsbUserPrincipalApi
import net.flipper.core.busylib.ktx.common.WrappedStateFlow
import net.flipper.core.busylib.ktx.common.wrap

class BUSYLibPrincipalApiImpl : BsbUserPrincipalApi {
    private val userStateFlow = MutableStateFlow<BsbUserPrincipal>(BsbUserPrincipal.Loading)

    override fun getPrincipalFlow(): WrappedStateFlow<BsbUserPrincipal> {
        return userStateFlow.wrap()
    }

    suspend fun update(userPrincipal: BsbUserPrincipal) {
        userStateFlow.emit(userPrincipal)
    }
}
