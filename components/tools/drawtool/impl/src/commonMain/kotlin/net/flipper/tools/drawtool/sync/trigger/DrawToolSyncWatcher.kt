package net.flipper.tools.drawtool.sync.trigger

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.storage.api.FStorageFeatureApi
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.launchIn
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import net.flipper.tools.drawtool.api.DrawToolSyncApi

/**
 * Runs a sync pass whenever a bar's storage comes up and again on every local
 * collection change while it stays up. A change during a running pass never
 * cancels it: the change waits, conflated to one, and runs as one follow-up
 * pass — only the storage going away cancels a pass, because it is doomed
 * anyway. A failed pass is retried with backoff a few times, then waits for
 * the next trigger; [DrawToolSyncApi.state] carries the failure to whoever
 * displays it.
 */
@Inject
@ContributesIntoSet(BusyLibGraph::class, binding<InternalBUSYLibStartupListener>())
class DrawToolSyncWatcher(
    scope: CoroutineScope,
    private val featureProvider: FFeatureProvider,
    private val collectionEvents: DrawToolCollectionEventConsumer,
    private val syncApi: DrawToolSyncApi,
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "DrawToolSyncWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    private suspend fun runSyncPass() {
        runSuspendCatching {
            exponentialRetry(retries = PASS_RETRIES) {
                syncApi.sync().toKotlinResult()
            }
        }.onFailure { throwable ->
            warn { "#runSyncPass gave up until the next trigger: $throwable" }
        }
    }

    override fun onLaunch() {
        info { "#onLaunch" }
        featureProvider.get<FStorageFeatureApi>()
            .map { featureStatus -> featureStatus is FFeatureStatus.Supported<*> }
            .distinctUntilChanged()
            .flatMapLatest { isStorageAvailable ->
                if (isStorageAvailable) {
                    collectionEvents.events
                        .onStart { emit(Unit) }
                        .conflate()
                        .onEach { _ -> runSyncPass() }
                } else {
                    emptyFlow()
                }
            }
            .launchIn(singleJobScope, SingleJobMode.CANCEL_PREVIOUS)
    }

    companion object {
        private const val PASS_RETRIES = 3L
    }
}
