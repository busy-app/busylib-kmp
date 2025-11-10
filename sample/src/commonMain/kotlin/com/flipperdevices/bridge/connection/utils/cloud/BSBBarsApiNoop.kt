package com.flipperdevices.bridge.connection.utils.cloud

import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipal
import com.flipperdevices.bsb.cloud.api.BSBBarsApi

class BSBBarsApiNoop : BSBBarsApi {
    override suspend fun registerBusyBar(
        principal: BsbUserPrincipal.Token,
        pin: String
    ): Result<Unit> {
        return Result.success(Unit)
    }
}
