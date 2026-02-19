package net.flipper.bridge.connection.config.api

import net.flipper.bridge.connection.config.api.model.BUSYBar

interface PersistedStorageTransactionScope {
    fun getCurrentDevice(): BUSYBar?
    fun getAllDevices(): List<BUSYBar>

    fun setCurrentDevice(id: String?)
    fun addOrReplace(device: BUSYBar)
    fun removeDevice(id: String)
}

fun PersistedStorageTransactionScope.getDevice(id: String): BUSYBar? {
    return getAllDevices().find { it.uniqueId == id }
}