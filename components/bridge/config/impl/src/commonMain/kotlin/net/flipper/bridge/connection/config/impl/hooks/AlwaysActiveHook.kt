package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.internal.HookPriority
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class AlwaysActiveHook : TransactionHook, LogTagProvider {
    override val TAG = "AlwaysActiveHook"

    override fun getPriority() = HookPriority.HIGH

    override fun InternalStorageTransactionScope.postTransaction() {
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
