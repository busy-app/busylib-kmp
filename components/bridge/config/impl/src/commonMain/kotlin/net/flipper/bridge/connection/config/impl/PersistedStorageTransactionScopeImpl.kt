package net.flipper.bridge.connection.config.impl

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn

class PersistedStorageTransactionScopeImpl(
    private var settings: BleConfigSettings
) : PersistedStorageTransactionScope, LogTagProvider {
    override val TAG = "PersistedStorageTransactionScope"
    override fun getCurrentDevice(): BUSYBar? {
        return settings.devices.find { it.uniqueId == settings.currentSelectedDeviceId }
    }

    override fun getAllDevices(): List<BUSYBar> {
        return settings.devices
    }

    override fun setCurrentDevice(id: String?) {
        settings = if (id == null) {
            settings.copy(currentSelectedDeviceId = null)
        } else if (settings.devices.none { it.uniqueId == id }) {
            error("Can't find device with id $id")
        } else {
            settings.copy(currentSelectedDeviceId = id)
        }
    }

    override fun addOrReplace(device: BUSYBar) {
        info { "Add device $device" }

        settings = settings.copy(
            devices = settings.devices.filter {
                it.uniqueId != device.uniqueId
            }.plus(device)
        )
    }

    override fun removeDevice(id: String) {
        val deviceExists = settings.devices.any { it.uniqueId == id }
        settings = if (!deviceExists) {
            warn { "Can't find device with id $id" }
            settings
        } else {
            settings.copy(
                devices = settings.devices.filter { it.uniqueId != id }
            )
        }
    }

    fun get() = settings
}