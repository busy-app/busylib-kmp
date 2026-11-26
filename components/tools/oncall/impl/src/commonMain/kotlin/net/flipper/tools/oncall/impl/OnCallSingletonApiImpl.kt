package net.flipper.tools.oncall.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.model.OnCallAnimation
import net.flipper.bridge.connection.feature.oncall.impl.OnCallDisplayLoop
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.getFiltered
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.tools.oncall.api.OnCallSingletonApi
import net.flipper.tools.oncall.impl.api.CloudAssetsApiLauncher
import net.flipper.tools.oncall.impl.model.CloudOnCallTarget

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding<OnCallSingletonApi>())
class OnCallSingletonApiImpl(
    scope: CoroutineScope,
    featureProvider: FFeatureProvider,
    private val devicePersistedStorage: FDevicePersistedStorage,
    orchestrator: FDeviceOrchestrator,
    private val cloudAssetsApiLauncher: CloudAssetsApiLauncher,
    private val displayLoopFactory: OnCallDisplayLoop.Factory
) : OnCallSingletonApi, LogTagProvider {
    override val TAG = "OnCallSingletonApi"

    private val singleJobScope = scope.asSingleJobScope()
    private val animationFlow = MutableStateFlow<OnCallAnimation?>(null)

    private val localRouteFlow: StateFlow<LocalRoute> = orchestrator.getState()
        .distinctUntilChangedBy { status -> status.tryCast<FDeviceConnectStatus.Connected>()?.deviceApi }
        .flatMapLatest { status -> status.toLocalRouteFlow(featureProvider) }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(), LocalRoute.Unknown)

    private fun FDeviceConnectStatus.toLocalRouteFlow(featureProvider: FFeatureProvider): Flow<LocalRoute> {
        if (this !is FDeviceConnectStatus.Connected) return flowOf(LocalRoute.None)
        return featureProvider.getFiltered<FOnCallFeatureApi>(this).map { featureStatus ->
            when (featureStatus) {
                is FFeatureStatus.Supported -> LocalRoute.Local(device.uniqueId, featureStatus.featureApi)
                FFeatureStatus.Retrieving -> LocalRoute.Resolving(device.uniqueId)
                FFeatureStatus.Unsupported, FFeatureStatus.NotFound -> LocalRoute.None
            }
        }
    }

    private fun LocalRoute.isCloudRouteFor(target: CloudOnCallTarget): Boolean? {
        return when (this) {
            LocalRoute.Unknown -> null
            is LocalRoute.Resolving -> if (uniqueId == target.uniqueId) null else true
            LocalRoute.None -> true
            is LocalRoute.Local -> uniqueId != target.uniqueId
        }
    }

    private fun List<BUSYBar>.isOnCallEnabled(uniqueId: String): Boolean {
        return any { busyBar -> busyBar.uniqueId == uniqueId && busyBar.onCallEnabled != false }
    }

    private fun List<BUSYBar>.toCloudOnCallTargets(): Set<CloudOnCallTarget> {
        return asSequence()
            .filter { busyBar -> busyBar.onCallEnabled != false }
            .mapNotNull { busyBar ->
                busyBar.cloud?.let { cloud -> CloudOnCallTarget(busyBar.uniqueId, cloud.deviceId) }
            }
            .toSet()
    }

    private suspend fun collectLocalOnCall() {
        localRouteFlow.collectLatest { localOnCall ->
            if (localOnCall !is LocalRoute.Local) return@collectLatest
            combine(
                flow = animationFlow,
                flow2 = devicePersistedStorage.getAllDevicesFlow(),
                transform = { animation, busyBars ->
                    animation?.takeIf { busyBars.isOnCallEnabled(localOnCall.uniqueId) }
                }
            ).distinctUntilChanged().collect { animation ->
                if (animation != null) localOnCall.featureApi.start(animation) else localOnCall.featureApi.stop()
            }
        }
    }

    private suspend fun runCloudOnCall(target: CloudOnCallTarget) {
        cloudAssetsApiLauncher.withAssetsApi(target.cloudDeviceId) { assetsApi ->
            val displayLoop = displayLoopFactory(assetsApi)
            var isDrawing = false
            try {
                localRouteFlow
                    .map { localRoute -> localRoute.isCloudRouteFor(target) }
                    .filterNotNull()
                    .distinctUntilChanged()
                    .collectLatest { isCloudRoute ->
                        isDrawing = isCloudRoute
                        if (isCloudRoute) {
                            animationFlow.filterNotNull().collectLatest { animation -> displayLoop.run(animation) }
                        }
                    }
            } finally {
                if (isDrawing) displayLoop.clear()
            }
        }.onFailure { t -> error(t) { "Cloud on-call failed for ${target.cloudDeviceId}" } }
    }

    private suspend fun collectCloudOnCall() {
        coroutineScope {
            val sessions = mutableMapOf<CloudOnCallTarget, Job>()
            combine(
                flow = animationFlow,
                flow2 = devicePersistedStorage.getAllDevicesFlow(),
                transform = { animation, busyBars ->
                    if (animation != null) busyBars.toCloudOnCallTargets() else emptySet()
                }
            ).distinctUntilChanged().collect { targets ->
                sessions.values.removeAll(Job::isCompleted)
                sessions.filterKeys { target -> target !in targets }.values.forEach(Job::cancel)
                targets.forEach { target ->
                    val previous = sessions[target]
                    if (previous?.isActive != true) {
                        sessions[target] = launch {
                            previous?.join()
                            runCloudOnCall(target)
                        }
                    }
                }
            }
        }
    }

    override fun start(animation: OnCallAnimation) {
        info { "Start on-call with $animation" }
        animationFlow.value = animation
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            launch { collectLocalOnCall() }
            launch { collectCloudOnCall() }
        }
    }

    override fun stop() {
        info { "Stop on-call" }
        animationFlow.value = null
    }

    private sealed interface LocalRoute {
        data object Unknown : LocalRoute
        data class Resolving(val uniqueId: String) : LocalRoute
        data object None : LocalRoute
        data class Local(val uniqueId: String, val featureApi: FOnCallFeatureApi) : LocalRoute
    }
}
