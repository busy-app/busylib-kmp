package net.flipper.bridge.connection.transport.combined.impl.utils

import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

object UpdateConfigDelegate : LogTagProvider {
    override val TAG = "UpdateConfigDelegate"

    internal suspend fun updateConnectionConfigUnsafe(
        oldConnections: List<AutoReconnectConnection>,
        config: FCombinedConnectionConfig,
        factory: (FDeviceConnectionConfig<*>) -> AutoReconnectConnection
    ): List<AutoReconnectConnection> {
        val matchedOldIndices = mutableSetOf<Int>()

        val newConnectionsList = config.connectionConfigs.map { newChildConfig ->
            resolveConnection(oldConnections, matchedOldIndices, newChildConfig, factory)
        }

        disconnectUnmatched(oldConnections, matchedOldIndices)

        return newConnectionsList
    }

    private suspend fun resolveConnection(
        oldConnections: List<AutoReconnectConnection>,
        matchedOldIndices: MutableSet<Int>,
        newChildConfig: FDeviceConnectionConfig<*>,
        factory: (FDeviceConnectionConfig<*>) -> AutoReconnectConnection
    ): AutoReconnectConnection {
        findExactMatch(oldConnections, matchedOldIndices, newChildConfig)?.let { return it }
        findUpdatableMatch(oldConnections, matchedOldIndices, newChildConfig)?.let { return it }
        info { "Create new connection for $newChildConfig" }
        return factory(newChildConfig)
    }

    private fun findExactMatch(
        oldConnections: List<AutoReconnectConnection>,
        matchedOldIndices: MutableSet<Int>,
        newChildConfig: FDeviceConnectionConfig<*>
    ): AutoReconnectConnection? {
        for ((idx, oldConn) in oldConnections.withIndex()) {
            if (idx in matchedOldIndices) continue
            if (oldConn.config == newChildConfig) {
                matchedOldIndices.add(idx)
                info { "Found exact match for $newChildConfig" }
                return oldConn
            }
        }
        return null
    }

    private suspend fun findUpdatableMatch(
        oldConnections: List<AutoReconnectConnection>,
        matchedOldIndices: MutableSet<Int>,
        newChildConfig: FDeviceConnectionConfig<*>
    ): AutoReconnectConnection? {
        for ((idx, oldConn) in oldConnections.withIndex()) {
            if (idx in matchedOldIndices) continue
            if (oldConn.tryUpdateConnectionConfig(newChildConfig).isSuccess) {
                matchedOldIndices.add(idx)
                info { "Successfully updated ${oldConn.config} with $newChildConfig" }
                return oldConn
            }
        }
        return null
    }

    private suspend fun disconnectUnmatched(
        oldConnections: List<AutoReconnectConnection>,
        matchedOldIndices: Set<Int>
    ) {
        for ((idx, oldConn) in oldConnections.withIndex()) {
            if (idx !in matchedOldIndices) {
                runSuspendCatching { oldConn.disconnect() }
            }
        }
    }
}
