package net.flipper.busylib.di

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.core.busylib.ktx.common.WrappedStateFlow
import net.flipper.core.busylib.ktx.common.wrap

class BUSYLibPrincipalApiImpl : BUSYLibPrincipalApi {
    private val userStateFlow = MutableStateFlow<BUSYLibUserPrincipal>(BUSYLibUserPrincipal.Loading)

    override fun getPrincipalFlow(): WrappedStateFlow<BUSYLibUserPrincipal> {
        return userStateFlow.wrap()
    }

    suspend fun update(userPrincipal: BUSYLibUserPrincipal) {
        userStateFlow.emit(userPrincipal)
    }
}
