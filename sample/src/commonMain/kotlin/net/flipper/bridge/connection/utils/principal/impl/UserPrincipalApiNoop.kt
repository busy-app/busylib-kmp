package net.flipper.bridge.connection.utils.principal.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.flipper.bsb.auth.principal.api.BsbUserPrincipal
import net.flipper.bsb.auth.principal.api.BsbUserPrincipalApi
import net.flipper.core.busylib.ktx.common.wrap

class UserPrincipalApiNoop : BsbUserPrincipalApi {
    val principalFlow = MutableStateFlow<BsbUserPrincipal>(BsbUserPrincipal.Empty)

    override fun getPrincipalFlow() = principalFlow.wrap()
}
