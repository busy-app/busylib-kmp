package net.flipper.bsb.auth.principal.api

import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface BUSYLibPrincipalApi {
    fun getPrincipalFlow(): WrappedStateFlow<BUSYLibUserPrincipal>
}
