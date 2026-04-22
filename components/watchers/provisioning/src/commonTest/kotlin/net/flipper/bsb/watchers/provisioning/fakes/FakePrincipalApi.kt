package net.flipper.bsb.watchers.provisioning.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.busylib.core.wrapper.wrap

internal class FakePrincipalApi(
    private val flow: MutableStateFlow<BUSYLibUserPrincipal>
) : BUSYLibPrincipalApi {
    override fun getPrincipalFlow() = flow.wrap()
}
