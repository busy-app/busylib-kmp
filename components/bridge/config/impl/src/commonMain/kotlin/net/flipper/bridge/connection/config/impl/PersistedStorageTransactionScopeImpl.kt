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

    override fun setCurrentDevice(device: BUSYBar?) {
        if (device == null) {
            settings = settings.copy(currentSelectedDeviceId = null)
            return
        }

        val isNewDevice = settings
            .devices
            .none { bUSYBar -> bUSYBar.uniqueId == device.uniqueId }

        if (isNewDevice) {
            addOrReplace(device)
        }

        settings = settings.copy(currentSelectedDeviceId = device.uniqueId)
    }

    override fun addOrReplace(device: BUSYBar) {
        info { "Add device $device" }

        settings = settings.copy(
            devices = settings.devices
                .filter { busyBar -> busyBar.uniqueId != device.uniqueId }
                .plus(device)
        )
    }

    override fun removeDevice(id: String) {
        val deviceExists = settings.devices.any { it.uniqueId == id }
        settings = if (!deviceExists) {
            warn { "Can't find device with id $id" }
            settings
        } else {
            settings.copy(
                devices = settings.devices.filter { busyBar -> busyBar.uniqueId != id },
                currentSelectedDeviceId = if (id == settings.currentSelectedDeviceId) {
                    null
                } else {
                    settings.currentSelectedDeviceId
                }
            )
        }
    }

    fun get() = settings
}
