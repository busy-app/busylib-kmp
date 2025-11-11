package net.flipper.bsb.cloud.api

import net.flipper.bsb.auth.principal.api.BsbUserPrincipal

interface BSBBarsApi {
    suspend fun registerBusyBar(
        principal: BsbUserPrincipal.Token,
        pin: String
    ): Result<Unit>
}
