package net.flipper.bridge.connection.config.api

import net.flipper.bridge.connection.config.api.model.BUSYBar

interface PersistedStorageTransactionScope {
    fun getCurrentDevice(): BUSYBar?
    fun getAllDevices(): List<BUSYBar>

    fun setCurrentDevice(device: BUSYBar?)
    fun addOrReplace(device: BUSYBar)
    fun removeDevice(id: String)
}

fun PersistedStorageTransactionScope.getDevice(id: String): BUSYBar? {
    return getAllDevices().find { busyBar -> busyBar.uniqueId == id }
}
