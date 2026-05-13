package net.flipper.bridge.device.firmwareupdate.status.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast

@Inject
class UpdateStatusProvider(
    private val fFeatureProvider: FFeatureProvider,
) {

    private fun UpdateStatusSource?.withLatestStatus(
        latestUpdateStatus: BsbUpdateStatus?
    ): UpdateStatusSource {
        return when (this) {
            null -> {
                UpdateStatusSource.Fresh(latestUpdateStatus)
            }

            is UpdateStatusSource.Cached -> {
                if (latestUpdateStatus == null) {
                    this
                } else {
                    UpdateStatusSource.Fresh(latestUpdateStatus)
                }
            }

            is UpdateStatusSource.Fresh -> {
                if (latestUpdateStatus == null) {
                    if (freshUpdateStatus == null) {
                        this
                    } else {
                        UpdateStatusSource.Cached(freshUpdateStatus)
                    }
                } else {
                    UpdateStatusSource.Fresh(latestUpdateStatus)
                }
            }
        }
    }

    fun getUpdateStatus(): Flow<UpdateStatusSource> = flow {
        var source: UpdateStatusSource = UpdateStatusSource.Fresh(null)
        emit(source)

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
