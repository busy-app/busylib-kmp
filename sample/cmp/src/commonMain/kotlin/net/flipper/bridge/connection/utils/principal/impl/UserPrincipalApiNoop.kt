package net.flipper.bridge.connection.utils.principal.impl

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.busylib.core.wrapper.wrap

class UserPrincipalApiNoop(
    defaultState: BUSYLibUserPrincipal = BUSYLibUserPrincipal.Empty
) : BUSYLibPrincipalApi {
    val principalFlow = MutableStateFlow(
        defaultState
    )

    override fun getPrincipalFlow() = principalFlow.wrap()
}
