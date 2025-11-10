package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.BUSYLibIOS
import com.flipperdevices.busylib.core.di.BusyLibGraph
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import platform.CoreBluetooth.CBCentralManager
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(BusyLibGraph::class)
@MergeComponent(BusyLibGraph::class)
abstract class BUSYLibGraphIOS(
    @get:Provides protected val scope: CoroutineScope,
    @get:Provides protected val principalApi: BsbUserPrincipalApi,
    @get:Provides protected val bsbBarsApi: BSBBarsApi,
    @get:Provides protected val persistedStorage: FDevicePersistedStorage,
    @get:Provides protected val manager: CBCentralManager,
) {
    abstract val busyLib: BUSYLibIOS
}

@MergeComponent.CreateComponent
expect fun create(
    scope: CoroutineScope,
    principalApi: BsbUserPrincipalApi,
    bsbBarsApi: BSBBarsApi,
    persistedStorage: FDevicePersistedStorage,
    manager: CBCentralManager,
): BUSYLibGraphIOS
