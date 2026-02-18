package net.flipper.bridge.connection.feature.events.impl.ws

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import net.flipper.bridge.connection.feature.events.api.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose

class WSEventsFlow : LogTagProvider {
    override val TAG = "WSEventsFlow"
    private val cache = UpdateEvent.entries.filter { it.webSocketKey != null }
        .associateBy { it.webSocketKey }

    fun getEventFlow(flow: Flow<TransportMetaInfoData?>): Flow<ConsumableUpdateEvent> {
        return flow.filterIsInstance<TransportMetaInfoData.StringValue>()
            .mapNotNull(::getConsumableUpdateEvent)
    }

    private fun getConsumableUpdateEvent(metaInfo: TransportMetaInfoData.StringValue): ConsumableUpdateEvent? {
        val event = cache[metaInfo.key]
        if (event == null) {
            verbose { "Unknown event: ${metaInfo.key}" }
            return null
        }
        return ConsumableUpdateEvent(
            updateEvent = event,
            value = metaInfo.value
        )
    }
}
