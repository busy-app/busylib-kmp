package net.flipper.bsb.watchers.desktop.hook

import net.flipper.bridge.connection.config.api.HookOrder
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.TransactionHook
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class DesktopAutoPurger : TransactionHook, LogTagProvider {
    override val TAG = "DesktopAutoPurger"

    override fun getPriority() = HookOrder.NORMAL

    override fun PersistedStorageTransactionScope.postTransaction() {
        val onlyLans = getAllDevices().filter { it.lan != null && it.connectionWays.size == 1 }
        if (onlyLans.isEmpty()) {
            return
        }
        val isCloudExist = getAllDevices().any { it.cloud != null }
        if (isCloudExist) {
            onlyLans.forEach {
                info { "Remove device $it because we have cloud devices in storage" }
                removeDevice(it.uniqueId)
            }
        } else {
            onlyLans.drop(1).forEach {
                info { "Remove device $it because this is lan duplicated" }
                removeDevice(it.uniqueId)
            }
        }
    }
}