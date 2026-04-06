package net.flipper.bridge.connection.transport.common.api.meta

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface FTransportMetaInfoApi {
    fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>>
}

fun FTransportMetaInfoApi?.getOrEmpty(key: TransportMetaInfoKey): Flow<TransportMetaInfoData?> {
    if (this == null) {
        return emptyFlow()
    }
    return get(key)
        .map { resultFlow -> resultFlow.getOrNull() }
        .flatMapLatest { flow -> flow ?: emptyFlow() }
}

fun FTransportMetaInfoApi?.getOrNullable(key: TransportMetaInfoKey): Flow<TransportMetaInfoData?> {
    if (this == null) {
        return flowOf(null)
    }
    return get(key)
        .map { resultFlow -> resultFlow.getOrNull() }
        .flatMapLatest { flow -> flow ?: flowOf(null) }
}
