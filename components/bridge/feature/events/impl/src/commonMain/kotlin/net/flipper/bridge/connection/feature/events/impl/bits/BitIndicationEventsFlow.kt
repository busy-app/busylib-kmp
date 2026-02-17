package net.flipper.bridge.connection.feature.events.impl.bits

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.feature.events.api.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error

class BitIndicationEventsFlow() {
    private val cache = UpdateEvent.entries.filter { it.bitIndex != null }
        .associateBy { it.bitIndex }

    fun getEventFlow(flow: Flow<TransportMetaInfoData?>): Flow<ConsumableUpdateEvent> {
        return flow.orEmpty()
            .onEach { debug { "Receive $it" } }
            .mapNotNull { data -> (data as? TransportMetaInfoData.RawBytes)?.bytes?.let(::parse) }
            .onEach { debug { "Receive updates: $it" } }
            .map { updateEvents ->
                updateEvents.map {
                    ConsumableUpdateEvent(
                        updateEvent = it,
                        value = null
                    )
                }
            }
            .flatMapLatest { updateEvents ->
                flow { updateEvents.forEach { event -> emit(event) } }
            }
    }

    private fun parse(byteArray: ByteArray): List<UpdateEvent> {
        val events = mutableListOf<UpdateEvent>()
        val bits = bitsOf(byteArray)
        bits.forEachBit { index ->
            val event = cache[index]
            if (event == null) {
                error { "Not found update event for index $index" }
            } else {
                events.add(event)
            }
        }

        return events
    }
}