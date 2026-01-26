package net.flipper.bsb.cloud.api

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.busylib.core.wrapper.CResult

interface BUSYLibBarsApi {
    suspend fun registerBusyBar(
        principal: BUSYLibUserPrincipal.Token,
        pin: String
    ): CResult<Unit>
}
