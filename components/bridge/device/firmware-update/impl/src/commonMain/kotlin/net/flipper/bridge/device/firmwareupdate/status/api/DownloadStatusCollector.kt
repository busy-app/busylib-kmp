package net.flipper.bridge.device.firmwareupdate.status.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.TickFlow
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.launchIn
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration.Companion.seconds

@Inject
class DownloadStatusCollector(
    private val fFeatureProvider: FFeatureProvider,
    scope: CoroutineScope
) : LogTagProvider by TaggedLogger("UpdaterStatusCollector") {
    private val singleJobScope = scope.asSingleJobScope()
    private val _isActiveFlow = MutableStateFlow(false)
    val isActiveFlow = _isActiveFlow

    fun start() {
        info { "#start" }
        TickFlow(UPDATE_DELAY)
            .onStart { _isActiveFlow.emit(true) }
            .throttleLatest {
                val updateStatus = exponentialRetry {
                    val fRpcFeatureApi = fFeatureProvider
                        .getSync<FRpcFeatureApi>()
                        ?: error("Could not get RPC feature api")
                    fRpcFeatureApi
                        .fRpcUpdaterApi
                        .getUpdateStatus(true)
                        .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
                }
                verbose { "Receive update status" }
                val downloadStateEvent = BusyLibUpdateEvent.Update.UpdateDownload(
                    speedBytesPerSec = updateStatus.install.download.speedBytesPerSec,
                    receivedBytes = updateStatus.install.download.receivedBytes,
                    totalBytes = updateStatus.install.download.totalBytes
                )
                val eventsFeatureApi = fFeatureProvider.getSync<FEventsFeatureApi>()
                eventsFeatureApi?.onBusyLibEvent(downloadStateEvent)
            }
            .onCompletion { _isActiveFlow.emit(false) }
            .launchIn(singleJobScope, SingleJobMode.CANCEL_PREVIOUS)
    }

    suspend fun stop() {
        info { "#stop" }
        singleJobScope.cancelPrevious().join()
    }

    companion object {
        private val UPDATE_DELAY = 3.seconds
    }
}
