package net.flipper.busylib.watchers.hook

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.TransactionHook
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class DesktopEmptyFiller : TransactionHook, LogTagProvider {
    override val TAG = "DesktopEmptyFiller"

    override fun PersistedStorageTransactionScope.postTransaction() {
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
