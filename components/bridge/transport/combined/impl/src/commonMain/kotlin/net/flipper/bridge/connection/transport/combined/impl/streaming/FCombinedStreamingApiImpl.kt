package net.flipper.bridge.connection.transport.combined.impl.streaming

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.transport.combined.impl.connections.SharedConnectionPool
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class FCombinedStreamingApiImpl(
    connectionPool: SharedConnectionPool
) : FStatusStreamingApi, LogTagProvider {
    override val TAG = "FCombinedStreamingApi"

    @OptIn(ExperimentalCoroutinesApi::class)
    private val delegates = connectionPool.get().map { list ->
        list.mapNotNull { it.status as? FInternalTransportConnectionStatus.Connected }
            .mapNotNull { it.deviceApi as? FStatusStreamingApi }
    }

    override fun getEvents(): Flow<StatusStreamingEvent> {
        return delegates.flatMapLatest { currentDelegates ->
            info { "Get status streaming from ${currentDelegates.size} delegates" }
            currentDelegates.firstOrNull()?.getEvents().orEmpty()
        }
    }
}
