package net.flipper.bridge.device.firmwareupdate.updater.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.onLatest
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class CheckUpdateService(
    private val fFeatureProvider: FFeatureProvider,
    private val scope: CoroutineScope
) : InternalBUSYLibStartupListener,
    LogTagProvider by TaggedLogger("CheckUpdateService") {
    override fun onLaunch() {
        fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .flatMapLatest { status ->
                status
                    .tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                    ?.featureApi
                    ?.updateStatusFlow
                    .orNullable()
            }
            .filter { status -> status?.check?.availableVersion.isNullOrEmpty() }
            .filter { status ->
                when (status?.check?.status) {
                    BsbUpdateStatus.BsbCheck.BsbCheckResult.AVAILABLE -> false
                    null,
                    BsbUpdateStatus.BsbCheck.BsbCheckResult.FAILURE,
                    BsbUpdateStatus.BsbCheck.BsbCheckResult.NONE,
                    BsbUpdateStatus.BsbCheck.BsbCheckResult.NOT_AVAILABLE -> true
                }
            }
            .flatMapLatest { fFeatureProvider.get<FRpcFeatureApi>() }
            .filterIsInstance<FFeatureStatus.Supported<*>>()
            .filter { fFeatureStatus -> fFeatureStatus.featureApi is FRpcFeatureApi }
            .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
            .map { status -> status.featureApi }
            .onLatest { featureApi ->
                exponentialRetry {
                    featureApi
                        .fRpcUpdaterApi
                        .startUpdateCheck()
                        .onFailure { throwable ->
                            error(throwable) {
                                "#startUpdateCheck could not start update check"
                            }
                        }
                        .map { }
                }
            }
            .launchIn(scope)
    }
}
