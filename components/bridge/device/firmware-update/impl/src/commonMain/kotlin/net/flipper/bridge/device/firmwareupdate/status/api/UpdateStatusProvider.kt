package net.flipper.bridge.device.firmwareupdate.status.api

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
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
    private val fDevicePersistedStorage: FDevicePersistedStorage
) {

    private fun UpdateStatusSource.withLatestStatus(
        latestUpdateStatus: BsbUpdateStatus?
    ): UpdateStatusSource {
        if (latestUpdateStatus != null) {
            return UpdateStatusSource.Fresh(latestUpdateStatus)
        }
        return when (this) {
            is UpdateStatusSource.Cached -> this
            is UpdateStatusSource.Fresh -> freshUpdateStatus
                ?.let(UpdateStatusSource::Cached)
                ?: this
        }
    }

    fun getUpdateStatus(): Flow<UpdateStatusSource> = channelFlow {
        var source: UpdateStatusSource = UpdateStatusSource.Fresh(null)
        send(source)
        fDevicePersistedStorage.getCurrentDeviceFlow()
            .distinctUntilChangedBy { busyBar -> busyBar?.uniqueId }
            .flatMapLatest {
                // Reset status on device change
                source = UpdateStatusSource.Fresh(null)
                send(source)

                fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
                    .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
                    .map { status -> status?.featureApi }
                    .flatMapLatest { feature -> feature?.updateStatusFlow.orNullable() }
                    .onEach { latestUpdateStatus ->
                        val localSource = source.withLatestStatus(latestUpdateStatus)
                        source = localSource
                        send(localSource)
                    }
            }
            .collect()

    }
}
