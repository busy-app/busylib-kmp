package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.BUSYLibDesktop
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope

@DependencyGraph(BusyLibGraph::class)
interface BUSYLibGraphDesktop {
    val busyLib: BUSYLibDesktop

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides scope: CoroutineScope,
            @Provides principalApi: BsbUserPrincipalApi,
            @Provides bsbBarsApi: BSBBarsApi,
            @Provides persistedStorage: FDevicePersistedStorage,
        ): BUSYLibGraphDesktop
    }
}
