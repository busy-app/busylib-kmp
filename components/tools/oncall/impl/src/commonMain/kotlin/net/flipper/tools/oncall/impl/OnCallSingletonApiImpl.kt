package net.flipper.tools.oncall.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.tools.oncall.api.OnCallSingletonApi

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<OnCallSingletonApi>())
class OnCallSingletonApiImpl(
    scope: CoroutineScope,
    featureProvider: FFeatureProvider,
) : OnCallSingletonApi, LogTagProvider {
    override val TAG = "OnCallSingletonApi"
    private val onCallFeatureApiFlow = featureProvider.get<FOnCallFeatureApi>()
        .filterIsInstance<FFeatureStatus.Supported<*>>()
        .filter { fFeatureStatus -> fFeatureStatus.featureApi is FOnCallFeatureApi }
        .filterIsInstance<FFeatureStatus.Supported<FOnCallFeatureApi>>()
        .stateIn(scope, SharingStarted.WhileSubscribed(0), null)

    private val singleJobScope = scope.asSingleJobScope()

    override fun start() {
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            info { "Subscribing to FOnCallFeatureApi" }
            onCallFeatureApiFlow
                .collectLatest { status ->
                    if (status != null) {
                        info { "FOnCallFeatureApi available, starting on-call" }
                        status.featureApi.start()
                    }
                }
        }
    }

    override fun stop() {
        singleJobScope.cancelPrevious()
        onCallFeatureApiFlow.value?.featureApi?.stop()
    }
}
