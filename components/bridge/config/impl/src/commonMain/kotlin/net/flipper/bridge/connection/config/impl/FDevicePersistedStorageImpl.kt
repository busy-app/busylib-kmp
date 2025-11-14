package net.flipper.bridge.connection.config.impl

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
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

    override fun getCurrentDevice(): Flow<FDeviceBaseModel?> {
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

    override suspend fun addDevice(device: FDeviceBaseModel) = bleConfigKrate.save { settings ->
        info { "Add device $device" }
        settings.copy(
            devices = settings.devices.plus(device)
        )
    }

    override suspend fun removeDevice(id: String) = bleConfigKrate.save { settings ->
        val deviceExists = settings.devices.any { it.uniqueId == id }
        if (!deviceExists) {
            warn { "Can't find device with id $id" }
            settings
        } else {
            settings.copy(
                devices = settings.devices.filter { it.uniqueId != id }
            )
        }
    }

    override fun getAllDevices(): Flow<List<FDeviceBaseModel>> {
        return bleConfigKrate.flow.map { it.devices }
    }

    override suspend fun updateCurrentDevice(
        block: (FDeviceBaseModel) -> FDeviceBaseModel
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
}
