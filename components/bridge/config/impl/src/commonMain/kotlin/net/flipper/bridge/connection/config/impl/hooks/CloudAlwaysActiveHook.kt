package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class CloudAlwaysActiveHook : TransactionHook, LogTagProvider {
    override val TAG = "CloudAlwaysActiveHook"

    override fun PersistedStorageTransactionScope.postTransaction() {
        // Set current active device
        if (getCurrentDevice() != null) {
            return
        }
        val cloud = getAllDevices().find { device ->
            device.connectionWays.any {
                it is BUSYBar.ConnectionWay.Cloud
            }
        } ?: return
        info { "Current device is null, set the first cloud device as current" }
        setCurrentDevice(cloud)
    }
}
