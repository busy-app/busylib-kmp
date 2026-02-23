package net.flipper.bridge.device.firmwareupdate.updater.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.onLatest
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class CheckUpdateService(
    private val fFeatureProvider: FFeatureProvider,
    private val scope: CoroutineScope
) : InternalBUSYLibStartupListener {
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
                    UpdateStatus.Check.CheckResult.AVAILABLE -> false
                    null,
                    UpdateStatus.Check.CheckResult.FAILURE,
                    UpdateStatus.Check.CheckResult.NONE,
                    UpdateStatus.Check.CheckResult.NOT_AVAILABLE -> true
                }
            }
            .onLatest {
                exponentialRetry {
                    fFeatureProvider.get<FRpcFeatureApi>()
                        .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
                        .first()
                        .featureApi
                        .fRpcUpdaterApi
                        .startUpdateCheck()
                        .onFailure { throwable ->
                            error(throwable) {
                                "#startUpdateCheck could not start update check"
                            }
                        }
                        .map { }
                        .toCResult()
                        .toKotlinResult()
                }
            }
            .launchIn(scope)
    }
}
