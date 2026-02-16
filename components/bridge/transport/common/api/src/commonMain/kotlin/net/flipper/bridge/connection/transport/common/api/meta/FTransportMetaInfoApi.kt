package net.flipper.bridge.connection.transport.common.api.meta

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

interface FTransportMetaInfoApi {
    fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>>
}

fun FTransportMetaInfoApi?.getOrEmpty(key: TransportMetaInfoKey): Flow<TransportMetaInfoData?> {
    if (this == null) {
        return emptyFlow()
    }
    return get(key)
        .flatMapLatest {
            it.getOrNull() ?: emptyFlow()
        }
}
