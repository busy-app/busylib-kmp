package net.flipper.bridge.connection.utils.principal.impl

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.auth.principal.api.BsbUserPrincipal
import net.flipper.bsb.auth.principal.api.BsbUserPrincipalApi

class UserPrincipalApiNoop : BsbUserPrincipalApi {
    override fun getPrincipalFlow() = MutableStateFlow(BsbUserPrincipal.Empty)
}
