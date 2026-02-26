package net.flipper.bridge.connection.feature.events.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import net.flipper.bridge.connection.feature.events.api.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.core.busylib.log.LogTagProvider
import kotlin.time.Duration.Companion.seconds

class FEventsFeatureApiImpl(
    scope: CoroutineScope
) : FEventsFeatureApi, LogTagProvider {
    override val TAG = "FEventsFeatureApi"

    private val sharedIndicationFlow = flow {
        while (currentCoroutineContext().isActive) {
            delay(5.seconds)
            UpdateEvent.entries
                .map { event -> ConsumableUpdateEvent(event, null) }
                .onEach { event -> emit(event) }
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5.seconds), 1)

    override fun getUpdatesFlow(): Flow<ConsumableUpdateEvent> = sharedIndicationFlow
}
