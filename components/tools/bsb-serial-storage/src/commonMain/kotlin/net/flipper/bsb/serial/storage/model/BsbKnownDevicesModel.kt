package net.flipper.bsb.serial.storage.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Persisted json of the known BUSY bars, holding the serial number of each of them
 */
@Serializable
data class BsbKnownDevicesModel(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("devices")
    val devices: List<BsbKnownDeviceEntry>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        val EMPTY: BsbKnownDevicesModel
            get() = BsbKnownDevicesModel(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                devices = emptyList()
            )
    }
}
