package net.flipper.bsb.serial.storage.api.internal

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import net.flipper.bsb.serial.storage.api.BsbDeviceSerialStore
import net.flipper.bsb.serial.storage.model.BsbKnownDeviceEntry
import net.flipper.bsb.serial.storage.model.BsbKnownDevicesModel
import net.flipper.core.busylib.data.di.qualifier.BusyLibJsonQualifier
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import ru.astrainteractive.klibs.kstorage.suspend.impl.DefaultSuspendMutableKrate

@Inject
class BsbDeviceSerialStoreImpl(
    private val settings: Settings,
    @BusyLibJsonQualifier private val json: Json,
) : BsbDeviceSerialStore, LogTagProvider {
    override val TAG = "BsbDeviceSerialStore"

    private val krate = DefaultSuspendMutableKrate(
        factory = { BsbKnownDevicesModel.EMPTY },
        loader = {
            runSuspendCatching {
                val storedKnownDevices = settings.getStringOrNull(KNOWN_DEVICES_KEY)
                    ?.takeIf(String::isNotBlank)
                    ?: return@runSuspendCatching null
                json.decodeFromString(BsbKnownDevicesModel.serializer(), storedKnownDevices)
            }.onFailure { parseError ->
                error(parseError) { "Could not read $KNOWN_DEVICES_KEY" }
            }.getOrNull()
        },
        saver = { knownDevicesModel ->
            runSuspendCatching {
                settings.putString(
                    key = KNOWN_DEVICES_KEY,
                    value = json.encodeToString(
                        BsbKnownDevicesModel.serializer(),
                        knownDevicesModel
                    )
                )
            }.onFailure { saveError ->
                error(saveError) { "Could not save $KNOWN_DEVICES_KEY" }
            }
        }
    )

    override suspend fun findSerial(deviceUniqueId: String): String? {
        return krate.getValue()
            .devices
            .firstOrNull { deviceEntry -> deviceEntry.uniqueId == deviceUniqueId }
            ?.serial
    }

    override suspend fun rememberSerial(
        deviceUniqueId: String,
        deviceSerial: String
    ) {
        return krate.save { knownDevicesModel ->
            val otherDevices = knownDevicesModel.devices
                .filterNot { deviceEntry -> deviceEntry.uniqueId == deviceUniqueId }
            val newEntry = BsbKnownDeviceEntry(
                uniqueId = deviceUniqueId,
                serial = deviceSerial
            )
            knownDevicesModel.copy(
                devices = otherDevices
                    .plus(newEntry)
                    .sortedBy { deviceEntry -> deviceEntry.uniqueId }
            )
        }
    }

    companion object {
        private const val KNOWN_DEVICES_KEY = "bsb_known_devices_v1"
    }
}
