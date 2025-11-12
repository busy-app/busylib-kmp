package net.flipper.bridge.connection.utils.principal.impl

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.busylib.core.wrapper.wrap

class UserPrincipalApiNoop : BUSYLibPrincipalApi {
    val principalFlow = MutableStateFlow<BUSYLibUserPrincipal>(BUSYLibUserPrincipal.Empty)

    override fun getPrincipalFlow() = principalFlow.wrap()
}
