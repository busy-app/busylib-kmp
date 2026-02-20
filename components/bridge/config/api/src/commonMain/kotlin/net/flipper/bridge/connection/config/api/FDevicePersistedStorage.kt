package net.flipper.bridge.connection.config.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FDevicePersistedStorage {
    fun getCurrentDeviceFlow(): WrappedFlow<BUSYBar?>
    fun getAllDevicesFlow(): WrappedFlow<List<BUSYBar>>

    suspend fun transaction(block: PersistedStorageTransactionScope.() -> Unit)
}
