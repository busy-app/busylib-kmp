package net.flipper.bsb.serial.storage.api

interface BsbDeviceSerialStore {
    suspend fun rememberSerial(
        deviceUniqueId: String,
        deviceSerial: String
    )

    suspend fun findSerial(deviceUniqueId: String): String?
}
