package com.flipperdevices.busylib.di

import android.content.Context
import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.BUSYLibAndroid
import com.flipperdevices.busylib.core.di.BusyLibGraph
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(BusyLibGraph::class)
@SingleIn(BusyLibGraph::class)
abstract class BUSYLibGraphAndroid(
    @get:Provides protected val scope: CoroutineScope,
    @get:Provides protected val principalApi: BsbUserPrincipalApi,
    @get:Provides protected val bsbBarsApi: BSBBarsApi,
    @get:Provides protected val persistedStorage: FDevicePersistedStorage,
    // Android-specific factory
    @get:Provides protected val context: Context,
) {
    abstract val busyLib: BUSYLibAndroid
}
