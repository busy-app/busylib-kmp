package com.flipperdevices.bridge.connection.screens.di

import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipal
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

// Temporary
@Inject
@ContributesBinding(BusyLibGraph::class, binding<BSBBarsApi>())
class BSBBarsApiNoop : BSBBarsApi {
    override suspend fun registerBusyBar(
        principal: BsbUserPrincipal.Token,
        pin: String
    ): Result<Unit> {
        return Result.success(Unit)
    }
}