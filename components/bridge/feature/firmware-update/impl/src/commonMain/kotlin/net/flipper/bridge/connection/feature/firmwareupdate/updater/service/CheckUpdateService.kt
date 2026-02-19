package net.flipper.bridge.connection.feature.firmwareupdate.updater.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
class CheckUpdateService(
    private val fFeatureProvider: FFeatureProvider,
    private val scope: CoroutineScope
) : LogTagProvider by TaggedLogger("CheckUpdateService") {
    fun onEnable() {
        fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .flatMapLatest { status ->
                status
                    .tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                    ?.featureApi
                    ?.getUpdateStatusFlow()
                    .orNullable()
            }
            .onEach { info { "#onEnable status changed: $it" } }
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
            .distinctUntilChanged()
            .throttleLatest {
                exponentialRetry {
                    info { "#onEnable getting update status" }
                    fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
                        .filterIsInstance<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                        .first()
                        .featureApi
                        .startUpdateCheck()
                        .toKotlinResult()
                }
            }
            .launchIn(scope)
    }
}
