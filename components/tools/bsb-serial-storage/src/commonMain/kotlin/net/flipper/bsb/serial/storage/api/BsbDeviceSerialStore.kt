package net.flipper.bsb.serial.storage.api

interface BsbDeviceSerialStore {
    suspend fun findSerial(deviceUniqueId: String): String?
}
