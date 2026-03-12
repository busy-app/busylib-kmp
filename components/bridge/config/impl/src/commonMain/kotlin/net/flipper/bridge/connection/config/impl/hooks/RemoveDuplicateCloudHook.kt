package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.TransactionHook
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class RemoveDuplicateCloudHook : TransactionHook, LogTagProvider {
    override val TAG = "RemoveDuplicateCloudHook"

    @Suppress("NestedBlockDepth")
    override fun PersistedStorageTransactionScope.postTransaction() {
        val allDevices = getAllDevices()
        val currentDevice = getCurrentDevice()
        val cloudDuplicatesDevices = allDevices
            .filter { it.cloud != null }
            .groupBy { it.cloud!!.deviceId }
            .values
            .filter { it.size > 1 }

        if (cloudDuplicatesDevices.isEmpty()) {
            return
        }
        info { "Found $cloudDuplicatesDevices duplicate cloud devices" }

        for (duplicates in cloudDuplicatesDevices) {
            val best = duplicates.maxBy { it.connectionWays.size }
            duplicates.forEach { device ->
                if (device.uniqueId != best.uniqueId) {
                    info {
                        "Removing duplicate device ${device.uniqueId} " +
                            "with cloud id ${device.cloud?.deviceId}, keeping ${best.uniqueId}"
                    }
                    removeDevice(device.uniqueId)
                    if (currentDevice?.uniqueId == device.uniqueId) {
                        info { "Switching current device to ${best.uniqueId}" }
                        setCurrentDevice(best)
                    }
                }
            }
        }
    }
}
