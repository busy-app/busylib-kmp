package net.flipper.busylib.watchers.hook

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.TransactionHook
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class DesktopAlwaysLan : TransactionHook, LogTagProvider {
    override val TAG = "DesktopAlwaysLan"
    override fun PersistedStorageTransactionScope.postTransaction() {
        getAllDevices().forEach { device ->
            if (device.lan == null) {
                info { "Found device without lan: $device, add them" }
                addOrReplace(device.copy(lan = BUSYBar.ConnectionWay.Lan()))
            }
        }
    }
}
