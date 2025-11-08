package com.flipperdevices.busylib.di

import android.content.Context
import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.BUSYLibAndroid
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope

@DependencyGraph(BusyLibGraph::class)
interface BUSYLibGraphAndroid {
    val busyLib: BUSYLibAndroid

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides scope: CoroutineScope,
            @Provides principalApi: BsbUserPrincipalApi,
            @Provides bsbBarsApi: BSBBarsApi,
            @Provides persistedStorage: FDevicePersistedStorage,
            // Android-specific factory
            @Provides context: Context,
        ): BUSYLibGraphAndroid
    }
}
