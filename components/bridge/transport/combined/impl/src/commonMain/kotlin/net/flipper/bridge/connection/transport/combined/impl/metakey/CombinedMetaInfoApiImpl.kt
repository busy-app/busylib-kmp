package net.flipper.bridge.connection.transport.combined.impl.metakey

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.transport.combined.impl.connections.SharedConnectionPool
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class CombinedMetaInfoApiImpl(
    connectionPool: SharedConnectionPool,
) : FTransportMetaInfoApi, LogTagProvider {
    override val TAG = "CombinedMetaInfoApi"

    @OptIn(ExperimentalCoroutinesApi::class)
    private val delegates = connectionPool.get().map { list ->
        list.mapNotNull { it.status as? FInternalTransportConnectionStatus.Connected }
            .mapNotNull { it.deviceApi as? FTransportMetaInfoApi }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>> {
        return delegates.flatMapLatest { currentDelegates ->
            info { "Get meta info key $key from ${currentDelegates.size} delegates" }

            val flows = currentDelegates.map { it.get(key) }
            if (flows.isEmpty()) {
                return@flatMapLatest flowOf(
                    Result.failure(
                        NoSuchElementException("No connected transport supports meta info key $key")
                    )
                )
            }

            combine(flows) { results ->
                results.find { it.isSuccess } ?: Result.failure(
                    NoSuchElementException("No connected transport supports meta info key $key")
                )
            }
        }
    }
}
