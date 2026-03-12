package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class AlwaysActiveHook : TransactionHook, LogTagProvider {
    override val TAG = "AlwaysActiveHook"

    override fun PersistedStorageTransactionScope.postTransaction() {
        if (getCurrentDevice() != null) {
            return
        }
        val devices = getAllDevices()
        val nextDevice = devices.firstOrNull { device ->
            device.cloud != null
        } ?: devices.firstOrNull() ?: return
        info { "Current device is null, selecting next available device: $nextDevice" }
        setCurrentDevice(nextDevice)
    }
}
