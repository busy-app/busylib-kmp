package net.flipper.bsb.watchers.desktop.hook

import net.flipper.bridge.connection.config.internal.HookPriority
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class DesktopActiveDevice : TransactionHook, LogTagProvider {
    override val TAG = "DesktopActiveDevice"

    override fun getPriority() = HookPriority.NORMAL

    override fun InternalStorageTransactionScope.postTransaction() {
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
