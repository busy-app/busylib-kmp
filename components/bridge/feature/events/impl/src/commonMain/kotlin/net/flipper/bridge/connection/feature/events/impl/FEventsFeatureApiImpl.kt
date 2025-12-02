package net.flipper.bridge.connection.feature.events.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

@Inject
class FEventsFeatureApiImpl(
    @Assisted private val metaInfoApi: FTransportMetaInfoApi,
) : FEventsFeatureApi, LogTagProvider {
    override val TAG = "FEventsFeatureApi"

    override fun getUpdatesFlow(): Flow<List<UpdateEvent>> {
        val indicationFlow = metaInfoApi.get(TransportMetaInfoKey.EVENTS_INDICATION)
            .onFailure {
                error(it) { "Failed receive ${TransportMetaInfoKey.EVENTS_INDICATION}" }
            }.getOrNull()
        if (indicationFlow == null) {
            return flowOf()
        }
        return indicationFlow.onEach {
            info { "Receive ${it?.toBitsString()}" }
        }.mapNotNull {
            it?.let(::parse)
        }.onEach {
            info { "Receive updates: $it" }
        }
    }

    private fun parse(byteArray: ByteArray): List<UpdateEvent> {
        val events = mutableListOf<UpdateEvent>()
        val bits = bitsOf(byteArray)
        bits.forEachBit { index ->
            val event = UpdateEvent.entries.getOrNull(index)
            if (event == null) {
                error { "Not found update event for index $index" }
            } else {
                events.add(event)
            }
        }

        return events
    }

    @Inject
    class InternalFactory(
        private val factory: (FTransportMetaInfoApi) -> FEventsFeatureApiImpl
    ) {
        operator fun invoke(
            metaInfoApi: FTransportMetaInfoApi
        ): FEventsFeatureApiImpl = factory(metaInfoApi)
    }
}
