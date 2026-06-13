package net.flipper.bridge.connection.feature.provider.impl.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.device.bsb.api.FBSBDeviceApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.busylib.core.di.BusyLibGraph
import kotlin.reflect.KClass

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<FFeatureProvider>())
class FFeatureProviderImpl(
    orchestrator: FDeviceOrchestrator,
    private val fBSBDeviceApiFactory: FBSBDeviceApi.Factory,
    globalScope: CoroutineScope
) : FFeatureProvider {
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
                val exceptionHandler = requireNotNull(
                    value = statusPairScope.coroutineContext[CoroutineExceptionHandler],
                    lazyMessage = { "The scope of statusPair should contain CoroutineExceptionHandler" }
                )
                val channelFlowJob = requireNotNull(coroutineContext[Job]) {
                    "channelFlow doesn't contains Job"
                }
                // SupervisorJob isolates child failures so they reach `exceptionHandler`
                // (statusPairScope's handler) instead of cancelling channelFlow upstream.
                // It is parented to channelFlow's Job so the captured scope dies when
                // either statusPairScope dies (channelFlow closes) or globalScope dies.
                val fBSBDeviceApi: FBSBDeviceApi = fBSBDeviceApiFactory(
                    scope = CoroutineScope(
                        coroutineContext + SupervisorJob(channelFlowJob) + exceptionHandler
                    ),
                    connectedDevice = deviceApi
                )
                withContext(statusPairScope.coroutineContext.minusKey(Job)) {
                    send(fBSBDeviceApi)
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
}
