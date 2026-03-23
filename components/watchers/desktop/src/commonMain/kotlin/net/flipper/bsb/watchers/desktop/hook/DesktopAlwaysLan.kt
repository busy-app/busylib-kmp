package net.flipper.bsb.watchers.desktop.hook

import net.flipper.bridge.connection.config.internal.HookPriority
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class DesktopAlwaysLan : TransactionHook, LogTagProvider {
    override val TAG = "DesktopAlwaysLan"

    override fun getPriority() = HookPriority.NORMAL

    override fun InternalStorageTransactionScope.postTransaction() {
        getAllDevices().forEach { device ->
            if (device.lan == null) {
                info { "Found device without lan: $device, add them" }
                addOrReplace(device.copy(lan = BUSYBar.ConnectionWay.Lan()))
            }
        }
    }
}
