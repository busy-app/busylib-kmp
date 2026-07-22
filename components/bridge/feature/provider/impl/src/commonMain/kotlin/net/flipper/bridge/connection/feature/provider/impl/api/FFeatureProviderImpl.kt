package net.flipper.bridge.connection.feature.provider.impl.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.device.bsb.api.FBSBDeviceApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.log.LogTagProvider
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding<FFeatureProvider>())
class FFeatureProviderImpl(
    orchestrator: FDeviceOrchestrator,
    private val fBSBDeviceApiFactory: FBSBDeviceApi.Factory,
    globalScope: CoroutineScope
) : FFeatureProvider, LogTagProvider {
    override val TAG = "FFeatureProvider"

    @Suppress("MemberVisibilityCanBePrivate")
    internal val deviceStateFlow = orchestrator
        .getState()
        .map { status ->
            when (status) {
                is FDeviceConnectStatus.Connected -> status.scope to status.deviceApi
                is FDeviceConnectStatus.Connecting,
                is FDeviceConnectStatus.Disconnecting,
                is FDeviceConnectStatus.Disconnected -> null
            }
        }
        .distinctUntilChanged()
        .flatMapLatest { statusPair ->
            if (statusPair == null) return@flatMapLatest flowOf(null)
            val (statusPairScope, deviceApi) = statusPair
            channelFlow<FBSBDeviceApi?> {
                withContext(statusPairScope.coroutineContext.minusKey(Job)) {
                    send(null)
                }
                // Parent to the device scope (== FDeviceHolder.scope): the orchestrator
                // disconnects via deviceScope.cancelAndJoin(), so features are fully destroyed
                // before the next device connects. SupervisorJob keeps a failing feature from
                // cancelling its siblings or this flow (it hits statusPairScope's handler).
                val featureScope = CoroutineScope(
                    statusPairScope.coroutineContext
                        .plus(SupervisorJob(statusPairScope.coroutineContext.job))
                )
                try {
                    // Don't attach to already dead scope
                    if (featureScope.isActive) {
                        featureScope.launchOnCompletion { trySend(null) }
                        val fBSBDeviceApi: FBSBDeviceApi = fBSBDeviceApiFactory(
                            scope = featureScope,
                            connectedDevice = deviceApi
                        )
                        withContext(statusPairScope.coroutineContext.minusKey(Job)) {
                            send(fBSBDeviceApi)
                        }
                    }
                    val statusPairScopeDisposable = statusPairScope.coroutineContext
                        .job
                        .invokeOnCompletion { t ->
                            trySend(null)
                            close(t)
                        }
                    awaitClose {
                        statusPairScopeDisposable.dispose()
                    }
                } finally {
                    withContext(NonCancellable) {
                        if (BuildKonfig.CRASH_APP_ON_FAILED_CHECKS) {
                            featureScope.coroutineContext.job.cancelAndJoin()
                        } else {
                            val isCompleted = withTimeoutOrNull(FEATURE_TEARDOWN_TIMEOUT) {
                                featureScope.coroutineContext.job.cancelAndJoin()
                            }
                            if (isCompleted == null) {
                                error {
                                    "Feature teardown timed out after $FEATURE_TEARDOWN_TIMEOUT, " +
                                        "a feature ignored cancellation"
                                }
                            }
                        }
                    }
                }
            }
        }
        .distinctUntilChanged()
        .stateIn(globalScope, SharingStarted.Eagerly, null)

    override fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>> {
        return deviceStateFlow.flatMapLatest { deviceApi ->
            if (deviceApi == null) {
                flowOf(FFeatureStatus.Retrieving)
            } else {
                flow<FFeatureStatus<T>> {
                    emit(FFeatureStatus.Retrieving)
                    val feature = deviceApi.get(clazz)?.await()
                    if (feature == null) {
                        emit(FFeatureStatus.Unsupported)
                    } else {
                        emit(FFeatureStatus.Supported(feature))
                    }
                }
            }
        }
    }

    override fun <T : FDeviceFeatureApi> getFiltered(
        status: FDeviceConnectStatus.Connected,
        clazz: KClass<T>
    ): Flow<FFeatureStatus<T>> {
        return deviceStateFlow.flatMapLatest { deviceApi ->
            if (deviceApi == null || deviceApi.isSame(status.deviceApi).not()) {
                flowOf(FFeatureStatus.Retrieving)
            } else {
                flow<FFeatureStatus<T>> {
                    emit(FFeatureStatus.Retrieving)
                    val feature = deviceApi.get(clazz)?.await()
                    if (feature == null) {
                        emit(FFeatureStatus.Unsupported)
                    } else {
                        emit(FFeatureStatus.Supported(feature))
                    }
                }
            }
        }
    }

    override suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T? {
        return get(clazz)
            .filter { it !is FFeatureStatus.Retrieving }
            .map { featureStatus ->
                when (featureStatus) {
                    is FFeatureStatus.Supported -> featureStatus.featureApi
                    FFeatureStatus.NotFound,
                    FFeatureStatus.Unsupported -> null

                    FFeatureStatus.Retrieving -> error("Impossible situation")
                }
            }.first()
    }

    companion object {
        private val FEATURE_TEARDOWN_TIMEOUT = 5.seconds
    }
}
