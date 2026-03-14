package net.flipper.busylib.watchers.hook

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.TransactionHook
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class DesktopActiveDevice : TransactionHook, LogTagProvider {
    override val TAG = "DesktopActiveDevice"

    override fun PersistedStorageTransactionScope.postTransaction() {
        val activeDevice = getCurrentDevice()
        if (activeDevice?.cloud != null) {
            return
        }
        getAllDevices().find { it.cloud != null }?.let { cloudDevice ->
            info { "Found device with cloud, switch to them" }
            setCurrentDevice(cloudDevice)
        }
    }
}
