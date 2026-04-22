package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.internal.HookPriority
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class RemoveDuplicateHardwareIdHook : TransactionHook, LogTagProvider {
    override val TAG = "RemoveDuplicateHardwareIdHook"

    override fun getPriority() = HookPriority.NORMAL

    @Suppress("NestedBlockDepth")
    override fun InternalStorageTransactionScope.postTransaction() {
        val allDevices = getAllDevices()
        val currentDevice = getCurrentDevice()
        val hwIdDuplicatesDevices = allDevices
            .filter { it.hardwareId != null }
            .groupBy { it.hardwareId }
            .values
            .filter { it.size > 1 }

        if (hwIdDuplicatesDevices.isEmpty()) {
            return
        }
        info { "Found $hwIdDuplicatesDevices duplicate hardware id devices" }

        for (duplicates in hwIdDuplicatesDevices) {
            var best = duplicates.maxBy { it.connectionWays.size }
            duplicates.forEach { device ->
                if (device.uniqueId != best.uniqueId) {
                    info {
                        "Removing duplicate device ${device.uniqueId} " +
                                "with hardware id ${device.hardwareId}, keeping ${best.uniqueId}"
                    }
                    removeDevice(device.uniqueId)
                    if (currentDevice?.uniqueId == device.uniqueId) {
                        info { "Switching current device to ${best.uniqueId}" }
                        setCurrentDevice(best)
                    }
                    best = mergeIfEmpty(best, device)
                }
            }
            addOrReplace(best)
        }
    }
}

internal fun mergeIfEmpty(original: BUSYBar, other: BUSYBar): BUSYBar {
    var result = original
    if (result.ble == null) {
        result = result.addTransport(ble = other.ble)
    }
    if (result.cloud == null) {
        result = result.addTransport(cloud = other.cloud)
    }
    if (result.lan == null) {
        result = result.addTransport(lan = other.lan)
    }
    return result
}
