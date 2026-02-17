package net.flipper.bridge.connection.transport.combined.impl.metakey

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class CombinedMetaInfoApiImpl(
    connectionsFlow: StateFlow<List<AutoReconnectConnection>>,
) : FTransportMetaInfoApi, LogTagProvider {
    override val TAG = "CombinedMetaInfoApi"

    @OptIn(ExperimentalCoroutinesApi::class)
    private val delegates = connectionsFlow.flatMapLatest { connections ->
        if (connections.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(connections.map { it.stateFlow }) { states ->
                states.filterIsInstance<FInternalTransportConnectionStatus.Connected>()
                    .map { it.deviceApi }
                    .filterIsInstance<FTransportMetaInfoApi>()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<ByteArray?>>> {
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
