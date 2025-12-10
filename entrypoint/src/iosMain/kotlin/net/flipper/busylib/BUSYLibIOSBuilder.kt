package net.flipper.busylib

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibBarsApi
import net.flipper.busylib.di.create
import platform.CoreBluetooth.CBCentralManager

actual class BUSYLibIOSBuilder(
    val scope: CoroutineScope,
    val principalApi: BUSYLibPrincipalApi,
    val busyLibBarsApi: BUSYLibBarsApi,
    val persistedStorage: FDevicePersistedStorage,
    val manager: CBCentralManager,
) {
    actual fun build(): BUSYLibIOS {
        val graph = create(
            scope,
            principalApi,
            busyLibBarsApi,
            persistedStorage,
            manager
        )
        return graph.busyLib
    }
}
