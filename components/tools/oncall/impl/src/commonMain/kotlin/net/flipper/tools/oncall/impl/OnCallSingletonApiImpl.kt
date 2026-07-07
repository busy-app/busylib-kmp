package net.flipper.tools.oncall.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.deviceOrNull
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.tools.oncall.api.OnCallSingletonApi
import net.flipper.tools.oncall.impl.session.CloudOnCallSession
import net.flipper.tools.oncall.impl.session.LanOnCallDeviceScanner
import net.flipper.tools.oncall.impl.session.LanOnCallSession
import net.flipper.tools.oncall.impl.session.OnCallSessionRoute

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding<OnCallSingletonApi>())
class OnCallSingletonApiImpl(
    scope: CoroutineScope,
    featureProvider: FFeatureProvider,
    private val devicePersistedStorage: FDevicePersistedStorage,
    private val orchestrator: FDeviceOrchestrator,
    private val lanDeviceScanner: LanOnCallDeviceScanner,
    private val cloudSessionFactory: CloudOnCallSession.Factory
) : OnCallSingletonApi, LogTagProvider {
    override val TAG = "OnCallSingletonApi"
    private val onCallFeatureApiFlow = featureProvider.get<FOnCallFeatureApi>()
        .filterIsInstance<FFeatureStatus.Supported<*>>()
        .filter { fFeatureStatus -> fFeatureStatus.featureApi is FOnCallFeatureApi }
        .filterIsInstance<FFeatureStatus.Supported<FOnCallFeatureApi>>()
        .stateIn(scope, SharingStarted.WhileSubscribed(0), null)

    private val singleJobScope = scope.asSingleJobScope()

    private suspend fun collectActiveDeviceOnCall() {
        info { "Subscribing to FOnCallFeatureApi" }
        onCallFeatureApiFlow
            .collectLatest { status ->
                if (status != null) {
                    info { "FOnCallFeatureApi available, starting on-call" }
                    status.featureApi.start()
                }
            }
    }

    private suspend fun runSession(route: OnCallSessionRoute) {
        when (route) {
            is OnCallSessionRoute.Lan -> LanOnCallSession(route.host).run()
            is OnCallSessionRoute.Cloud -> cloudSessionFactory.invoke(route.deviceId).run()
        }
    }

    private fun getBackgroundOnCallRoutesFlow(): Flow<Set<OnCallSessionRoute>> {
        return combine(
            flow = devicePersistedStorage.getAllDevicesFlow(),
            flow2 = orchestrator.getState(),
            flow3 = lanDeviceScanner.getLanHostsFlow(),
            transform = { devices, connectStatus, lanHosts ->
                val lanRoutes = lanHosts.map { host -> OnCallSessionRoute.Lan(host) }
                val cloudRoutes = devices
                    .asSequence()
                    .filter { busyBar -> busyBar.onCallEnabled == true }
                    .filter { busyBar -> busyBar.uniqueId != connectStatus.deviceOrNull?.uniqueId }
                    .mapNotNull { busyBar -> busyBar.cloud?.deviceId }
                    .map { deviceId -> OnCallSessionRoute.Cloud(deviceId) }
                cloudRoutes.plus(lanRoutes).toSet()
            }
        ).distinctUntilChanged()
    }

    private suspend fun collectBackgroundDevicesOnCall() {
        getBackgroundOnCallRoutesFlow()
            .collectLatest { routes ->
                coroutineScope {
                    routes.map { route ->
                        info { "Starting background on-call for $route" }
                        async {
                            runSuspendCatching { runSession(route) }
                                .onFailure { t -> error(t) { "Background on-call session failed for $route" } }
                                .getOrNull()
                        }
                    }.awaitAll()
                }
            }
    }

    override fun start() {
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            launch { collectActiveDeviceOnCall() }
            launch { collectBackgroundDevicesOnCall() }
        }
    }

    override fun stop() {
        singleJobScope.cancelPrevious()
        onCallFeatureApiFlow.value?.featureApi?.stop()
    }
}
