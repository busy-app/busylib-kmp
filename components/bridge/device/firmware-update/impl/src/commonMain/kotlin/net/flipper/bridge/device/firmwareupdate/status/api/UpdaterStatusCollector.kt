package net.flipper.bridge.device.firmwareupdate.status.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.model.BsbUpdateEvent
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.TickFlow
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.ktx.common.launchIn
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds

@Inject
class UpdaterStatusCollector(
    private val fFeatureProvider: FFeatureProvider,
    private val scope: CoroutineScope
) : LogTagProvider by TaggedLogger("UpdaterStatusCollector") {
    private val singleJobScope = scope.asSingleJobScope()

    fun start() {
        info { "#start" }
        TickFlow(UPDATE_DELAY)
            .flatMapLatest { fFeatureProvider.get<FEventsFeatureApi>() }
            .map { status -> status.tryCast<FFeatureStatus.Supported<FEventsFeatureApi>>() }
            .filterNotNull()
            .map { status -> status.featureApi }
            .onEach { eventsFeatureApi ->
                info { "#start sent UPDATER_UPDATE_STATUS" }
                eventsFeatureApi.onBsbEvent(BsbUpdateEvent.UPDATER_UPDATE_STATUS)
            }
            .launchIn(singleJobScope, SingleJobMode.CANCEL_PREVIOUS)
    }

    fun stop(graceful: Boolean = false) {
        info { "#stop graceful: $graceful" }
        scope.launch {
            if (graceful) delay(UPDATE_DELAY)
            singleJobScope.cancelPrevious()
        }
    }

    companion object {
        private val UPDATE_DELAY = 3.seconds
    }
}
