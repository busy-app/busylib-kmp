package net.flipper.bridge.device.firmwareupdate.updater.api

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bridge.device.firmwareupdate.updater.model.FirmwareUpdateCheckError
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.busylib.core.wrapper.CResult
import kotlin.time.Duration.Companion.seconds

@Inject
class AwaitingFirmwareUpdateChecker(
    private val firmwareUpdaterApi: Lazy<FirmwareUpdaterApi>,
    private val delegate: FirmwareUpdateChecker,
    private val orchestrator: FDeviceOrchestrator
) : FirmwareUpdateChecker {
    private fun isCheckCompleted(state: FwUpdateState): Boolean {
        return when (state) {
            FwUpdateState.NoUpdateAvailable,
            FwUpdateState.UpdateAvailable,
            FwUpdateState.BatteryLow,
            FwUpdateState.CouldNotCheckUpdate -> true
            FwUpdateState.Pending,
            FwUpdateState.CheckingVersion,
            FwUpdateState.Updating,
            FwUpdateState.Preparing,
            FwUpdateState.DownloadFailure,
            is FwUpdateState.Uploading,
            is FwUpdateState.Downloading -> false
        }
    }

    private fun toCheckResult(state: FwUpdateState): CResult<Unit> {
        return when (state) {
            FwUpdateState.NoUpdateAvailable,
            FwUpdateState.UpdateAvailable -> CResult.success(Unit)
            FwUpdateState.BatteryLow -> CResult.failure(FirmwareUpdateCheckError.BatteryLow())
            FwUpdateState.CouldNotCheckUpdate -> CResult.failure(FirmwareUpdateCheckError.CouldNotCheckUpdate())
            FwUpdateState.Pending,
            FwUpdateState.CheckingVersion,
            FwUpdateState.Updating,
            FwUpdateState.Preparing,
            FwUpdateState.DownloadFailure,
            is FwUpdateState.Uploading,
            is FwUpdateState.Downloading -> error("Update check has not completed: $state")
        }
    }

    override suspend fun checkForUpdates(): CResult<Unit> {
        return coroutineScope {
            val connection = orchestrator.getState().value
            val isLanConnection = when (connection) {
                is FDeviceConnectStatus.Connected -> connection.transportType == FDeviceTransportType.LAN
                else -> false
            }
            val resultAsync = async(start = CoroutineStart.UNDISPATCHED) {
                firmwareUpdaterApi.value.state
                    .drop(if (isLanConnection) 0 else 1)
                    .first(::isCheckCompleted)
                    .let(::toCheckResult)
            }
            try {
                val startResult = delegate.checkForUpdates()
                if (startResult is CResult.Failure) return@coroutineScope startResult
                withTimeoutOrNull(RESULT_TIMEOUT) { resultAsync.await() }
                    ?: CResult.failure(FirmwareUpdateCheckError.Timeout())
            } finally {
                resultAsync.cancel()
            }
        }
    }

    private companion object {
        val RESULT_TIMEOUT = 10.seconds
    }
}
