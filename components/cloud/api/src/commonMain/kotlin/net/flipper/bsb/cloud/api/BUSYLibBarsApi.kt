package net.flipper.bsb.cloud.api

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal

interface BUSYLibBarsApi {
    suspend fun registerBusyBar(
        principal: BUSYLibUserPrincipal.Token,
        pin: String
    ): Result<Unit>
}
