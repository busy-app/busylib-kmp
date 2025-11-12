package net.flipper.bsb.auth.principal.api

import net.flipper.core.busylib.ktx.common.WrappedStateFlow

interface BUSYLibPrincipalApi {
    fun getPrincipalFlow(): WrappedStateFlow<BUSYLibUserPrincipal>
}
