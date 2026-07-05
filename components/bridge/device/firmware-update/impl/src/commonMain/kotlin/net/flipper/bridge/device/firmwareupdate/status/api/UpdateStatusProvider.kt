package net.flipper.bridge.device.firmwareupdate.status.api

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.getFiltered
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.info

data class DeviceUpdateVersion(
    val deviceId: String?,
    val version: BsbUpdateVersion?,
)

@Inject
class UpdateStatusProvider(
    private val fFeatureProvider: FFeatureProvider,
    private val fDevicePersistedStorage: FDevicePersistedStorage,
    private val fDeviceOrchestrator: FDeviceOrchestrator,
) : LogTagProvider by TaggedLogger("UpdateStatusProvider") {

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

    fun getUpdateStatus(): Flow<UpdateStatusSource> {
        return fDevicePersistedStorage.getCurrentDeviceFlow()
            .map { device -> device?.uniqueId }
            .distinctUntilChanged()
            .flatMapLatest { deviceId -> updateStatusForDevice(deviceId) }
    }

    fun getUpdateVersion(): Flow<BsbUpdateVersion?> {
        return getUpdateVersionKeyed()
            .map { keyed -> keyed.version }
            .distinctUntilChanged()
    }

    /**
     * [getUpdateVersion] plus the device id the version belongs to.
     */
    fun getUpdateVersionKeyed(): Flow<DeviceUpdateVersion> {
        return fDevicePersistedStorage.getCurrentDeviceFlow()
            .map { device -> device?.uniqueId }
            .distinctUntilChanged()
            .flatMapLatest { deviceId ->
                firmwareFeatureFlow(deviceId)
                    .flatMapLatest { feature -> feature?.updateVersionFlow.orNullable() }
                    .map { version -> DeviceUpdateVersion(deviceId, version) }
            }
            .distinctUntilChanged()
    }

    private fun updateStatusForDevice(deviceId: String?): Flow<UpdateStatusSource> = flow {
        info { "#updateStatusForDevice reset source for deviceId=$deviceId" }
        var source: UpdateStatusSource = UpdateStatusSource.Fresh(null)
        emit(source)

        firmwareFeatureFlow(deviceId)
            .flatMapLatest { feature -> feature?.updateStatusFlow.orNullable() }
            .onEach { latestUpdateStatus ->
                val localSource = source.withLatestStatus(latestUpdateStatus)
                debug {
                    "#updateStatusForDevice deviceId=$deviceId latest=$latestUpdateStatus " +
                        "source: $source -> $localSource"
                }
                source = localSource
                emit(localSource)
            }
            .collect()
    }

    /**
     * The firmware-update feature of the connection that belongs to [deviceId],
     * or `null` while that device is not connected.
     * Gating by `Connected.device.uniqueId` drops emissions of any foreign connection.
     */
    private fun firmwareFeatureFlow(deviceId: String?): Flow<FFirmwareUpdateFeatureApi?> {
        return fDeviceOrchestrator.getState()
            .map { status ->
                (status as? FDeviceConnectStatus.Connected)
                    ?.takeIf { connected -> connected.device.uniqueId == deviceId }
            }
            .distinctUntilChanged()
            .flatMapLatest { connected ->
                if (connected == null) {
                    flowOf(null)
                } else {
                    fFeatureProvider.getFiltered<FFirmwareUpdateFeatureApi>(connected)
                        .map { status ->
                            status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()?.featureApi
                        }
                }
            }
            .distinctUntilChanged()
            .onEach { feature ->
                debug {
                    "#firmwareFeatureFlow deviceId=$deviceId " +
                        if (feature == null) "no matching connection" else "connection matched"
                }
            }
    }
}
