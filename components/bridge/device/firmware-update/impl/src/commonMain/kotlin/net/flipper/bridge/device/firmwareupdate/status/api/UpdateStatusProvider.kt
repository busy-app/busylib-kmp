package net.flipper.bridge.device.firmwareupdate.status.api

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.DeviceUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.DeviceUpdateVersion
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.info

@Inject
class UpdateStatusProvider(
    private val fFeatureProvider: FFeatureProvider,
) : LogTagProvider by TaggedLogger("UpdateStatusProvider") {

    private fun UpdateStatusSource.withLatestStatus(
        latestUpdateStatus: DeviceUpdateStatus?
    ): UpdateStatusSource {
        return when (this) {
            is UpdateStatusSource.Cached -> {
                if (latestUpdateStatus == null) {
                    this
                } else {
                    UpdateStatusSource.Fresh(latestUpdateStatus)
                }
            }

            is UpdateStatusSource.Fresh -> {
                if (latestUpdateStatus == null) {
                    if (status == null) {
                        this
                    } else {
                        UpdateStatusSource.Cached(status)
                    }
                } else {
                    UpdateStatusSource.Fresh(latestUpdateStatus)
                }
            }
        }
    }

    fun getUpdateStatus(): Flow<UpdateStatusSource> {
        return firmwareFeatureFlow()
            .flatMapLatest { feature -> feature?.updateStatusFlow ?: flowOf(null) }
            .runningFold<DeviceUpdateStatus?, UpdateStatusSource>(
                initial = UpdateStatusSource.Fresh(null)
            ) { source, latest ->
                val isNewDevice = latest != null && latest.deviceId != source.status?.deviceId
                val localSource = if (isNewDevice) {
                    info { "#getUpdateStatus reset source for new deviceId=${latest.deviceId}" }
                    UpdateStatusSource.Fresh(latest)
                } else {
                    source.withLatestStatus(latest)
                }
                debug { "#getUpdateStatus latest=$latest source: $source -> $localSource" }
                localSource
            }
            .distinctUntilChanged()
    }

    /**
     * [DeviceUpdateVersion] of the current connection, `null` while disconnected
     */
    fun getUpdateVersion(): Flow<DeviceUpdateVersion?> {
        return firmwareFeatureFlow()
            .flatMapLatest { feature -> feature?.updateVersionFlow ?: flowOf(null) }
            .distinctUntilChanged()
    }

    /**
     * The firmware-update feature of the current connection, or `null` while disconnected
     */
    private fun firmwareFeatureFlow(): Flow<FFirmwareUpdateFeatureApi?> {
        return fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status ->
                status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()?.featureApi
            }
            .distinctUntilChanged()
            .onEach { feature ->
                debug {
                    "#firmwareFeatureFlow " +
                        if (feature == null) "no connection" else "feature for deviceId=${feature.deviceId}"
                }
            }
    }
}
