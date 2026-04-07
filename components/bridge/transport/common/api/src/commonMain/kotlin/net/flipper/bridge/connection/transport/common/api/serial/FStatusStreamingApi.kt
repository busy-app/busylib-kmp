package net.flipper.bridge.connection.transport.common.api.serial

import kotlinx.coroutines.flow.Flow

sealed interface StatusStreamingEvent {
    class Protobuf(
        val data: ByteArray
    ) : StatusStreamingEvent
}

interface FStatusStreamingApi {
    fun getEvents(): Flow<StatusStreamingEvent>
}
