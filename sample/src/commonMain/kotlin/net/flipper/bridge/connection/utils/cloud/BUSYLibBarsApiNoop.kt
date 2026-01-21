package net.flipper.bridge.connection.utils.cloud

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibBarsApi
import net.flipper.busylib.core.wrapper.CResult

class BUSYLibBarsApiNoop : BUSYLibBarsApi {
    override suspend fun registerBusyBar(
        principal: BUSYLibUserPrincipal.Token,
        pin: String
    ): CResult<Unit> {
        return CResult.success(Unit)
    }
}
