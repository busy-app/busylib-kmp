package net.flipper.bridge.connection.config.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel

interface FDevicePersistedStorage {
    fun getCurrentDevice(): Flow<FDeviceBaseModel?>
    suspend fun setCurrentDevice(id: String?)
    suspend fun addDevice(device: FDeviceBaseModel)
    suspend fun removeDevice(id: String)
    fun getAllDevices(): Flow<Set<FDeviceBaseModel>>
    suspend fun updateCurrentDevice(block: (FDeviceBaseModel) -> FDeviceBaseModel)

    suspend fun updateDevice(id: String, block: (FDeviceBaseModel) -> FDeviceBaseModel)
}
