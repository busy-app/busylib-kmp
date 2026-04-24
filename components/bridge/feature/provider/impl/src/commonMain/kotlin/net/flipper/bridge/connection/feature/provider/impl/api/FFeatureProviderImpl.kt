package net.flipper.bridge.connection.feature.provider.impl.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.device.bsb.api.FBSBDeviceApi
import net.flipper.bridge.connection.device.common.api.FDeviceApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.createLinkedScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.KClass

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, FFeatureProvider::class)
class FFeatureProviderImpl(
    private val orchestrator: FDeviceOrchestrator,
    private val fBSBDeviceApiFactory: FBSBDeviceApi.Factory,
    globalScope: CoroutineScope
) : FFeatureProvider {
    private val deviceStateFlow = MutableStateFlow<FDeviceApi?>(null)

    init {
        globalScope.launch {
            orchestrator
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
                .collectLatest { statusPair ->
                    statusPair?.let {
                        coroutineScope {
                            val (scope, deviceApi) = statusPair
                            val childScope =
                                createLinkedScope(scopeFirst = scope, scopeSecond = this)
                            deviceStateFlow.emit(fBSBDeviceApiFactory(childScope, deviceApi))
                            try {
                                awaitCancellation()
                            } finally {
                                withContext(NonCancellable) {
                                    deviceStateFlow.emit(null)
                                }
                            }
                        }
                    }
                }
        }
    }

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
