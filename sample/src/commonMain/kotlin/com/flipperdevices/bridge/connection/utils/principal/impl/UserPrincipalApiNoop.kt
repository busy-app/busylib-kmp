package com.flipperdevices.bridge.connection.utils.principal.impl

import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipal
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import kotlinx.coroutines.flow.MutableStateFlow

class UserPrincipalApiNoop : BsbUserPrincipalApi {
    override fun getPrincipalFlow() = MutableStateFlow(BsbUserPrincipal.Empty)
}
