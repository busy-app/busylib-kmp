package net.flipper.bridge.connection.config.api

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FDevicePersistedStorage {
    fun getCurrentDeviceFlow(): WrappedFlow<BUSYBar?>
    fun getAllDevicesFlow(): WrappedFlow<List<BUSYBar>>

    suspend fun <T> transaction(block: suspend PersistedStorageTransactionScope.() -> T): T
}
