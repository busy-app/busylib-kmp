package net.flipper.bridge.connection.config.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.config.api.model.BUSYBar

interface FDevicePersistedStorage {
    fun getCurrentDevice(): Flow<BUSYBar?>
    suspend fun setCurrentDevice(id: String?)
    suspend fun addDevice(device: BUSYBar)
    suspend fun removeDevice(id: String)
    fun getAllDevices(): Flow<List<BUSYBar>>
    suspend fun updateCurrentDevice(block: (BUSYBar) -> BUSYBar)
}
