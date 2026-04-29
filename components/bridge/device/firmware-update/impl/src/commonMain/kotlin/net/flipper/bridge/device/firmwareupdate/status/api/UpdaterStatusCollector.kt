package net.flipper.bridge.device.firmwareupdate.status.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.device.firmwareupdate.status.mapper.toBsbUpdateStatus
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.TickFlow
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.launchIn
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
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
            .throttleLatest { eventsFeatureApi ->
                info { "#start sent UPDATER_UPDATE_STATUS" }
                val updateStatus = exponentialRetry {
                    val fRpcFeatureApi = fFeatureProvider.getSync<FRpcFeatureApi>()
                        ?: error("Could not get RPC feature api")
                    fRpcFeatureApi
                        .fRpcUpdaterApi
                        .getUpdateStatus(true)
                        .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
                }.toBsbUpdateStatus()
                val updateStateEvent = BusyLibUpdateEvent.Update.UpdateState(
                    action = updateStatus.install.action,
                    status = updateStatus.install.status,
                )
                eventsFeatureApi.onBusyLibEvent(updateStateEvent)
                val downloadStateEvent = BusyLibUpdateEvent.Update.UpdateDownload(
                    download = updateStatus.install.download,
                )
                eventsFeatureApi.onBusyLibEvent(downloadStateEvent)
            }
            .launchIn(singleJobScope, SingleJobMode.CANCEL_PREVIOUS)
    }

    fun stop(graceful: Boolean = false) {
        info { "#stop graceful: $graceful" }
        scope.launch {
            if (graceful) delay(UPDATE_DELAY)
            singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
                val downloadStateEvent = BusyLibUpdateEvent.Update.UpdateDownload(
                    download = BsbUpdateStatus.BsbInstall.BsbDownload.ZERO,
                )
                fFeatureProvider.getSync<FEventsFeatureApi>()?.onBusyLibEvent(downloadStateEvent)
            }
        }
    }

    companion object {
        private val UPDATE_DELAY = 3.seconds
    }
}
