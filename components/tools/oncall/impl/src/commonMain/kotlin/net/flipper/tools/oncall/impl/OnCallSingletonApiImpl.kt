package net.flipper.tools.oncall.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.oncall.impl.OnCallDisplayLoop
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getFilteredFeature
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
    private val isOnCallFlow = MutableStateFlow(false)

    private val localOnCallFlow: StateFlow<LocalOnCall?> = orchestrator.getState()
        .distinctUntilChangedBy { status -> status.tryCast<FDeviceConnectStatus.Connected>()?.deviceApi }
        .flatMapLatest { status -> featureProvider.getFilteredFeature<FOnCallFeatureApi>(status) }
        .map { feature ->
            feature?.let { (featureApi, status) -> LocalOnCall(status.device.uniqueId, featureApi) }
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(), null)

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
        localOnCallFlow.collectLatest { localOnCall ->
            if (localOnCall == null) return@collectLatest
            combine(
                flow = isOnCallFlow,
                flow2 = devicePersistedStorage.getAllDevicesFlow(),
                transform = { isOnCall, busyBars -> isOnCall && busyBars.isOnCallEnabled(localOnCall.uniqueId) }
            ).distinctUntilChanged().collect { isShown ->
                if (isShown) localOnCall.featureApi.start() else localOnCall.featureApi.stop()
            }
        }
    }

    private suspend fun runCloudOnCall(target: CloudOnCallTarget) {
        cloudAssetsApiLauncher.withAssetsApi(target.cloudDeviceId) { assetsApi ->
            val displayLoop = displayLoopFactory(assetsApi)
            var isDrawing = false
            try {
                localOnCallFlow
                    .map { localOnCall -> localOnCall?.uniqueId != target.uniqueId }
                    .distinctUntilChanged()
                    .collectLatest { isCloudRoute ->
                        isDrawing = isCloudRoute
                        if (isCloudRoute) displayLoop.run()
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
                flow = isOnCallFlow,
                flow2 = devicePersistedStorage.getAllDevicesFlow(),
                transform = { isOnCall, busyBars -> if (isOnCall) busyBars.toCloudOnCallTargets() else emptySet() }
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

    override fun start() {
        info { "Start on-call" }
        isOnCallFlow.value = true
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            launch { collectLocalOnCall() }
            launch { collectCloudOnCall() }
        }
    }

    override fun stop() {
        info { "Stop on-call" }
        isOnCallFlow.value = false
    }

    private data class LocalOnCall(
        val uniqueId: String,
        val featureApi: FOnCallFeatureApi
    )
}
