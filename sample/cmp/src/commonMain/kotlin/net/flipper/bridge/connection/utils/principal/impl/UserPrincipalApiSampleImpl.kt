package net.flipper.bridge.connection.utils.principal.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.busylib.core.wrapper.wrap

class UserPrincipalApiSampleImpl(
    scope: CoroutineScope,
    private val hostApi: BUSYLibHostApi
) : BUSYLibPrincipalApi {
    private val principalFlow = MutableStateFlow<BUSYLibUserPrincipal>(BUSYLibUserPrincipal.Loading)

    init {
        scope.launch {
            principalFlow.emit(getUserPrincipal(hostApi))
        }
    }

    override fun getPrincipalFlow() = principalFlow.wrap()
}