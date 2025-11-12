package net.flipper.busylib.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibBarsApi
import net.flipper.busylib.BUSYLibDesktop
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(BusyLibGraph::class)
@MergeComponent(BusyLibGraph::class)
abstract class BUSYLibGraphDesktop(
    @get:Provides protected val scope: CoroutineScope,
    @get:Provides protected val principalApi: BUSYLibPrincipalApi,
    @get:Provides protected val busyLibBarsApi: BUSYLibBarsApi,
    @get:Provides protected val persistedStorage: FDevicePersistedStorage,
) {
    abstract val busyLib: BUSYLibDesktop
}
