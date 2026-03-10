package net.flipper.bridge.connection.feature.events.impl.ws

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import net.flipper.bridge.connection.feature.events.model.BsbUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose

class WSEventsFlow : LogTagProvider {
    override val TAG = "WSEventsFlow"
    private val cache = BsbUpdateEvent.entries.filter { it.webSocketKey != null }
        .associateBy { it.webSocketKey }

    fun getEventFlow(flow: Flow<TransportMetaInfoData?>): Flow<ConsumableUpdateEvent.Bsb> {
        return flow.filterIsInstance<TransportMetaInfoData.StringValue>()
            .mapNotNull(::getConsumableUpdateEvent)
    }

    private fun getConsumableUpdateEvent(
        metaInfo: TransportMetaInfoData.StringValue
    ): ConsumableUpdateEvent.Bsb? {
        val event = cache[metaInfo.key]
        if (event == null) {
            verbose { "Unknown event: ${metaInfo.key}" }
            return null
        }
        return ConsumableUpdateEvent.Bsb(
            bsbUpdateEvent = event,
            value = metaInfo.value
        )
    }
}
