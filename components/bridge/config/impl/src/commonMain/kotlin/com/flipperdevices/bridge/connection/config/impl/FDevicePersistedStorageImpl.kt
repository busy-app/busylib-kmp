package com.flipperdevices.bridge.connection.config.impl

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import com.flipperdevices.core.di.AppGraph
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.core.log.info
import com.flipperdevices.core.log.warn
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.astrainteractive.klibs.kstorage.suspend.FlowMutableKrate
import ru.astrainteractive.klibs.kstorage.util.save

@Inject
@ContributesBinding(AppGraph::class, binding = binding<FDevicePersistedStorage>())
class FDevicePersistedStorageImpl(
    private val bleConfigKrate: FlowMutableKrate<BleConfigSettings>
) : FDevicePersistedStorage, LogTagProvider {
    override val TAG = "FDevicePersistedStorage"

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
        val devicesList = settings.devices.toMutableList()
        val deviceIndex = devicesList.indexOfFirst { it.uniqueId == id }
        if (deviceIndex < 0) {
            warn { "Can't find device with id $id" }
            settings
        } else {
            devicesList.removeAt(deviceIndex)
            settings.copy(
                devices = devicesList
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
