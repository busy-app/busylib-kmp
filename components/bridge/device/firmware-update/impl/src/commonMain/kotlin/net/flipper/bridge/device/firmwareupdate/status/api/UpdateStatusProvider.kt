package net.flipper.bridge.device.firmwareupdate.status.api

import jdk.jfr.internal.OldObjectSample.emit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast

@Inject
class UpdateStatusProvider(
    private val fFeatureProvider: FFeatureProvider,
) {

    private fun UpdateStatusSource?.withLatestStatus(
        latestUpdateStatus: UpdateStatus?
    ): UpdateStatusSource {
        return when (val localSource = this) {
            null -> {
                UpdateStatusSource.Fresh(latestUpdateStatus)
            }

            is UpdateStatusSource.Cached -> {
                val freshUpdateStatus = localSource.freshUpdateStatus
                if (freshUpdateStatus == null) {
                    localSource.copy(freshUpdateStatus = latestUpdateStatus)
                } else {
                    localSource.copy(
                        cachedUpdateStatus = freshUpdateStatus,
                        freshUpdateStatus = latestUpdateStatus
                    )
                }
            }

            is UpdateStatusSource.Fresh -> {
                val freshUpdateStatus = localSource.freshUpdateStatus
                if (freshUpdateStatus == null) {
                    localSource.copy(freshUpdateStatus = latestUpdateStatus)
                } else {
                    UpdateStatusSource.Cached(
                        cachedUpdateStatus = freshUpdateStatus,
                        freshUpdateStatus = latestUpdateStatus
                    )
                }
            }
        }
    }

    fun getUpdateStatus(): Flow<UpdateStatusSource> = flow {
        var source: UpdateStatusSource? = null

        fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.updateStatusFlow.orNullable() }
            .onEach { latestUpdateStatus ->
                val localSource = source.withLatestStatus(latestUpdateStatus)
                source = localSource
                emit(localSource)
            }
            .collect()
    }
}
