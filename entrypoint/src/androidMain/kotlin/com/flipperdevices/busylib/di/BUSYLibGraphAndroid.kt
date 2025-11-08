package com.flipperdevices.busylib.di

import android.content.Context
import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.BUSYLibAndroid
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.flipperdevices.busylib.core.di.SingleIn
import com.r0adkll.kimchi.annotations.MergeComponent
import kotlinx.coroutines.CoroutineScope

@MergeComponent(BusyLibGraph::class)
@SingleIn(BusyLibGraph::class)
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