package net.flipper.bridge.connection.transport.combined.impl.metakey

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class CombinedMetaInfoApiImpl(
    scope: CoroutineScope,
    connections: List<AutoReconnectConnection>,
) : FTransportMetaInfoApi, LogTagProvider {
    override val TAG = "CombinedMetaInfoApi"

    private val delegates = combine(connections.map { it.stateFlow }) { states ->
        states.filterIsInstance<FInternalTransportConnectionStatus.Connected>()
            .map { it.deviceApi }
            .filterIsInstance<FTransportMetaInfoApi>()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun get(key: TransportMetaInfoKey): Result<Flow<ByteArray?>> {
        val currentDelegates = delegates.value
        info { "Get meta info key $key from ${currentDelegates.size} delegates" }

        for (delegate in currentDelegates) {
            val result = delegate.get(key)
            if (result.isSuccess) {
                return result
            }
        }

        return Result.failure(
            NoSuchElementException("No connected transport supports meta info key $key")
        )
    }
}