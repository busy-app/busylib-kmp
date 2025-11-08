package com.flipperdevices.bsb.cloud.api

import com.flipperdevices.busylib.principal.api.BsbUserPrincipal

interface BSBBarsApi {
    suspend fun registerBusyBar(
        principal: BsbUserPrincipal.Token,
        pin: String
    ): Result<Unit>
}
