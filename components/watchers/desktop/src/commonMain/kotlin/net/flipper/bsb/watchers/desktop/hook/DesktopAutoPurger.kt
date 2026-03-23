package net.flipper.bsb.watchers.desktop.hook

import net.flipper.bridge.connection.config.internal.HookPriority
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class DesktopAutoPurger : TransactionHook, LogTagProvider {
    override val TAG = "DesktopAutoPurger"

    override fun getPriority() = HookPriority.NORMAL

    override fun InternalStorageTransactionScope.postTransaction() {
        val onlyLansOrEmpty = getAllDevices()
            .filter { it.connectionWays.isEmpty() || (it.lan != null && it.connectionWays.size == 1) }
        if (onlyLansOrEmpty.isEmpty()) {
            return
        }
        val isCloudExist = getAllDevices().any { it.cloud != null }
        val listToDelete = if (isCloudExist) {
            onlyLansOrEmpty.onEach {
                info { "Remove device $it because we have cloud devices in storage" }
            }
        } else {
            val currentDevice = getCurrentDevice()
            val deviceToKeep = onlyLansOrEmpty
                .firstOrNull { it.uniqueId == currentDevice?.uniqueId }
                ?: onlyLansOrEmpty.first()
            onlyLansOrEmpty
                .filter { it.uniqueId != deviceToKeep.uniqueId }
                .onEach {
                    info { "Remove device $it because this is lan duplicated" }
                }
        }
        listToDelete.forEach {
            removeDevice(it.uniqueId)
        }
    }
}
