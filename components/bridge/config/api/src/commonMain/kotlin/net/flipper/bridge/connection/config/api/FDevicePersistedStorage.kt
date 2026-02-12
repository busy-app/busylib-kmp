package net.flipper.bridge.connection.config.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.config.api.model.FDeviceCombined

interface FDevicePersistedStorage {
    fun getCurrentDevice(): Flow<FDeviceCombined?>
    suspend fun setCurrentDevice(id: String?)
    suspend fun addDevice(device: FDeviceCombined)
    suspend fun removeDevice(id: String)
    fun getAllDevices(): Flow<List<FDeviceCombined>>
    suspend fun updateCurrentDevice(block: (FDeviceCombined) -> FDeviceCombined)
}
