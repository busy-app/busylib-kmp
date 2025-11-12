package net.flipper.bridge.connection.utils.principal.impl

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal

class UserPrincipalApiNoop : BUSYLibPrincipalApi {
    override fun getPrincipalFlow() = MutableStateFlow(BUSYLibUserPrincipal.Empty)
}
