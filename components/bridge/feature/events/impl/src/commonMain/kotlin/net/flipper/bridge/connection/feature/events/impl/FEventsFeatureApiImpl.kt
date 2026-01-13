package net.flipper.bridge.connection.feature.events.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds

@Inject
class FEventsFeatureApiImpl(
    @Assisted private val metaInfoApi: FTransportMetaInfoApi,
    @Assisted private val scope: CoroutineScope
) : FEventsFeatureApi, LogTagProvider {
    override val TAG = "FEventsFeatureApi"

    private val sharedIndicationFlow = flow {
        metaInfoApi.get(TransportMetaInfoKey.EVENTS_INDICATION)
            .onFailure { error(it) { "Failed receive ${TransportMetaInfoKey.EVENTS_INDICATION}" } }
            .getOrNull()
            .orEmpty()
            .onEach { info { "Receive ${it?.toBitsString()}" } }
            .mapNotNull { byteArray -> byteArray?.let(::parse) }
            .onEach { info { "Receive updates: $it" } }
            .map { updateEvents -> updateEvents.map(::ConsumableUpdateEvent) }
            .collect { updateEvents -> updateEvents.forEach { event -> emit(event) } }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5.seconds))

    override fun getUpdatesFlow(): Flow<ConsumableUpdateEvent> = sharedIndicationFlow

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
        private val factory: (FTransportMetaInfoApi, CoroutineScope) -> FEventsFeatureApiImpl
    ) {
        operator fun invoke(
            metaInfoApi: FTransportMetaInfoApi,
            scope: CoroutineScope
        ): FEventsFeatureApiImpl = factory(metaInfoApi, scope)
    }
}
