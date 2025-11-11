package net.flipper.bridge.connection.utils.cloud

import net.flipper.bsb.auth.principal.api.BsbUserPrincipal
import net.flipper.bsb.cloud.api.BSBBarsApi

class BSBBarsApiNoop : BSBBarsApi {
    override suspend fun registerBusyBar(
        principal: BsbUserPrincipal.Token,
        pin: String
    ): Result<Unit> {
        return Result.success(Unit)
    }
}
