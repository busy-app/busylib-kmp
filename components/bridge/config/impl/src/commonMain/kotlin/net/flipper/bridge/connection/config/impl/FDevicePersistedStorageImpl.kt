package net.flipper.bridge.connection.config.impl

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import ru.astrainteractive.klibs.kstorage.util.save

class FDevicePersistedStorageImpl(
    private val bleConfigKrate: BleConfigSettingsKrate
) : FDevicePersistedStorage, LogTagProvider {
    override val TAG = "FDevicePersistedStorage"

    constructor(
        observableSettings: ObservableSettings
    ) : this(BleConfigSettingsKrateImpl(observableSettings))

    override fun getCurrentDevice(): Flow<BUSYBar?> {
        return bleConfigKrate.flow.map { config ->
            val deviceId = config.currentSelectedDeviceId
            if (deviceId.isNullOrBlank()) {
                return@map null
            } else {
                config.devices.find { it.uniqueId == deviceId }
            }
        }
    }

    override suspend fun setCurrentDevice(id: String?) = bleConfigKrate.save { settings ->
        if (id == null) {
            settings.copy(currentSelectedDeviceId = null)
        } else if (settings.devices.none { it.uniqueId == id }) {
            error("Can't find device with id $id")
        } else {
            settings.copy(currentSelectedDeviceId = id)
        }
    }

    override suspend fun addDevice(device: BUSYBar) = bleConfigKrate.save { settings ->
        info { "Add device $device" }

        settings.copy(
            devices = settings.devices.filter {
                it.uniqueId != device.uniqueId
            }.plus(device)
        )
    }

    override suspend fun unpairDevice(device: BUSYBar): Result<Unit> {
        val isDeviceExists = bleConfigKrate.getValue()
            .devices
            .firstOrNull { listDevice -> listDevice.uniqueId == device.uniqueId } != null
        if (!isDeviceExists) {
            warn { "#unpairDevice Can't find device $device" }
            Result.success(Unit)
        }
        val hasCloudConnection = device.connectionWays
            .filterIsInstance<BUSYBar.ConnectionWay.Cloud>()
            .isNotEmpty()
        if (hasCloudConnection) {
            // todo unlink request
        }

        bleConfigKrate.save { settings ->
            val devices = settings
                .devices
                .filter { listDevice -> listDevice.uniqueId != device.uniqueId }
            settings.copy(
                devices = devices,
                currentSelectedDeviceId = devices
                    .firstOrNull()
                    ?.uniqueId,
            )
        }
        return Result.success(Unit)
    }

    override fun getAllDevices(): Flow<List<BUSYBar>> {
        return bleConfigKrate.flow.map { it.devices }
    }

    override suspend fun updateCurrentDevice(
        block: (BUSYBar) -> BUSYBar
    ) = bleConfigKrate.save { settings ->
        settings.copy(
            devices = settings.devices
                .map { device ->
                    if (device.uniqueId == settings.currentSelectedDeviceId) {
                        block(device)
                    } else {
                        device
                    }
                }
        )
    }

    override suspend fun updateDevice(id: String, block: (BUSYBar) -> BUSYBar) {
        bleConfigKrate.save { settings ->
            settings.copy(
                devices = settings.devices
                    .map { device ->
                        if (device.uniqueId == id) {
                            block(device)
                        } else {
                            device
                        }
                    }
            )
        }
    }
}
