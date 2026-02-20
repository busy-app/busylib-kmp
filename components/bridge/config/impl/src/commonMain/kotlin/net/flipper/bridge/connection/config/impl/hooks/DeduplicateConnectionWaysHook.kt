package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.BUSYBar.ConnectionWay
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.warn

class DeduplicateConnectionWaysHook : TransactionHook, LogTagProvider {
    override val TAG = "DeduplicateConnectionWaysHook"

    override fun PersistedStorageTransactionScope.postTransaction() {
        getAllDevices().filter { device ->
            device.connectionWays.groupBy { it::class }.any { it.value.size > 1 }
        }.forEach { device ->
            val deduplicated = device.connectionWays.distinct()
            warn { "Found duplicate connection ways for ${device.uniqueId}, deduplicating ${device.connectionWays.size} -> ${deduplicated.size}" }
            addOrReplace(device.copy(connectionWays = deduplicated))
        }
    }
}