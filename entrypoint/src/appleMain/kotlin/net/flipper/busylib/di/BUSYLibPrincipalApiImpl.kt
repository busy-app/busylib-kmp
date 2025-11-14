package net.flipper.busylib.di

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap

class BUSYLibPrincipalApiImpl : BUSYLibPrincipalApi {
    private val userStateFlow = MutableStateFlow<BUSYLibUserPrincipal>(BUSYLibUserPrincipal.Empty)

    override fun getPrincipalFlow(): WrappedStateFlow<BUSYLibUserPrincipal> {
        return userStateFlow.wrap()
    }

    suspend fun update(userPrincipal: BUSYLibUserPrincipal) {
        userStateFlow.emit(userPrincipal)
    }
}
