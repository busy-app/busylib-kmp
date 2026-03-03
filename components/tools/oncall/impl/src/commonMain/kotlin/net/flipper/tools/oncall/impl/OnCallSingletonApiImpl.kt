package net.flipper.tools.oncall.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
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
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, OnCallSingletonApi::class)
class OnCallSingletonApiImpl(
    scope: CoroutineScope,
    featureProvider: FFeatureProvider,
) : OnCallSingletonApi, LogTagProvider {
    override val TAG = "OnCallSingletonApi"
    private val onCallFeatureApiFlow = featureProvider.get<FOnCallFeatureApi>()
        .filterIsInstance<FFeatureStatus.Supported<FOnCallFeatureApi>>()
        .stateIn(scope, SharingStarted.WhileSubscribed(0), null)

    private val singleJobScope = scope.asSingleJobScope()

    override fun start() {
        singleJobScope.withJobMode(SingleJobMode.CANCEL_PREVIOUS) {
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
