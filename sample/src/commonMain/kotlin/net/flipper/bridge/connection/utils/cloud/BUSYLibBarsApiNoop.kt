package net.flipper.bridge.connection.utils.cloud

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibBarsApi

class BUSYLibBarsApiNoop : BUSYLibBarsApi {
    override suspend fun registerBusyBar(
        principal: BUSYLibUserPrincipal.Token,
        pin: String
    ): Result<Unit> {
        return Result.success(Unit)
    }
}
