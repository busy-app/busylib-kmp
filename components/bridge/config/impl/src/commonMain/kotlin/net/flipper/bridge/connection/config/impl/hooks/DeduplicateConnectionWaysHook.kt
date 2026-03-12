package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.TransactionHook
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.warn

class DeduplicateConnectionWaysHook : TransactionHook, LogTagProvider {
    override val TAG = "DeduplicateConnectionWaysHook"

    /**
     * @return devices that have multiple connection ways of the same type
     */
    private fun PersistedStorageTransactionScope.getDevicesWithDuplicateConnectionWays(): List<BUSYBar> {
        return getAllDevices().filter { device ->
            device.connectionWays
                .groupBy { connectionWay -> connectionWay::class }
                .any { (_, connectionWays) -> connectionWays.size > 1 }
        }
    }

    override fun PersistedStorageTransactionScope.postTransaction() {
        getDevicesWithDuplicateConnectionWays().forEach { device ->
            val deduplicated = device.connectionWays.distinct()
            warn {
                "Found duplicate connection ways for ${device.uniqueId}, " +
                    "deduplicating ${device.connectionWays.size} -> ${deduplicated.size}"
            }
            addOrReplace(device.copy(connectionWays = deduplicated))
        }
    }
}
