package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.BUSYLibDesktop
import com.flipperdevices.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import com.r0adkll.kimchi.annotations.MergeComponent
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides

@SingleIn(BusyLibGraph::class)
@MergeComponent(BusyLibGraph::class)
abstract class BUSYLibGraphDesktop(
    @get:Provides protected val scope: CoroutineScope,
    @get:Provides protected val principalApi: BsbUserPrincipalApi,
    @get:Provides protected val bsbBarsApi: BSBBarsApi,
    @get:Provides protected val persistedStorage: FDevicePersistedStorage,
) {
    abstract val busyLib: BUSYLibDesktop
}