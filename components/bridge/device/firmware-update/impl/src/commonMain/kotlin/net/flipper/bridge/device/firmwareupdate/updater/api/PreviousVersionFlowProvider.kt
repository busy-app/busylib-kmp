package net.flipper.bridge.device.firmwareupdate.updater.api

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.device.firmwareupdate.updater.diff.VersionsModelDiff
import net.flipper.bridge.device.firmwareupdate.updater.model.BusyBarVersionTransition
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast

@Inject
class PreviousVersionFlowProvider(
    private val fFeatureProvider: FFeatureProvider
) {
    private fun getPreviousVersionFlow() = flow(
        block = {
            emit(null)
            var busyBarVersionTransitionOrNull: BusyBarVersionTransition? = null
            fFeatureProvider.get<FDeviceInfoFeatureApi>()
                .map { status -> status.tryCast<FFeatureStatus.Supported<FDeviceInfoFeatureApi>>() }
                .flatMapLatest { status -> status?.featureApi?.deviceVersionFlow.orNullable() }
                .distinctUntilChanged()
                .onEach { newCurrentVersion ->
                    val versionModel = VersionsModelDiff.compareAndGet(
                        localVersionModelOrNull = busyBarVersionTransitionOrNull,
                        newCurrentVersion = newCurrentVersion
                    )
                    busyBarVersionTransitionOrNull = versionModel
                    emit(versionModel)
                }
                .collect()
        }
    )

    /**
     * After each successful update/upload/download, we need to restart previous version checker
     */
    fun getAutoRestartedPreviousVersionFlow(fwUpdateStateFlow: Flow<FwUpdateState>): Flow<BusyBarVersionTransition?> {
        return channelFlow {
            var job: Job? = null
            fwUpdateStateFlow
                .distinctUntilChangedBy { fwUpdateState ->
                    when (fwUpdateState) {
                        is FwUpdateState.Downloading -> 0

                        is FwUpdateState.Uploading,
                        is FwUpdateState.Updating -> 1

                        else -> 1
                    }
                }
                .collect { _ ->
                    job?.cancelAndJoin()
                    job = getPreviousVersionFlow()
                        .onEach { versionsModel -> send(versionsModel) }
                        .launchIn(this)
                }
            awaitClose()
        }
    }
}
