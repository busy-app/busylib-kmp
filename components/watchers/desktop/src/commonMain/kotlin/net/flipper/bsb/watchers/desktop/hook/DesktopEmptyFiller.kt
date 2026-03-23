package net.flipper.bsb.watchers.desktop.hook

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.internal.HookPriority
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class DesktopEmptyFiller : TransactionHook, LogTagProvider {
    override val TAG = "DesktopEmptyFiller"

    override fun getPriority() = HookPriority.NORMAL

    override fun InternalStorageTransactionScope.postTransaction() {
        if (getAllDevices().isEmpty()) {
            info { "Found no devices, add BUSY Bar LAN" }
            val device = BUSYBar(
                humanReadableName = "BUSY Bar LAN",
                lan = BUSYBar.ConnectionWay.Lan()
            )
            setCurrentDevice(device)
        }
    }
}
