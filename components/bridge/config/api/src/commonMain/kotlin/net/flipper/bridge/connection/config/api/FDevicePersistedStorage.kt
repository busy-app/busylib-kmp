package net.flipper.bridge.connection.config.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.config.api.model.BUSYBar

interface FDevicePersistedStorage {
    fun getCurrentDeviceFlow(): Flow<BUSYBar?>
    fun getAllDevicesFlow(): Flow<List<BUSYBar>>

    suspend fun transaction(block: PersistedStorageTransactionScope.() -> Unit)
}
