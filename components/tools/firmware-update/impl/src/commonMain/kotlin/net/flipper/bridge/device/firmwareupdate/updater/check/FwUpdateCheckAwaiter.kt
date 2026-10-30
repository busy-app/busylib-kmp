package net.flipper.bridge.device.firmwareupdate.updater.check

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateCheckResult
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds

private val RESULT_TIMEOUT = 10.seconds

internal object FwUpdateCheckAwaiter : LogTagProvider by TaggedLogger("FwUpdateCheckAwaiter") {

    /**
     * Subscribes to [state] before [startCheck], so a result that arrives while the request
     * is still running is not lost. [state] first replays the state from before the check,
     * so it is skipped unless [skipsCurrentState] is false: on LAN the update version is read
     * over REST and the check does not change the state.
     *
     * The timeout starts after [startCheck] returns.
     */
    suspend fun await(
        state: StateFlow<FwUpdateState>,
        skipsCurrentState: Boolean,
        startCheck: suspend () -> CResult<Unit>
    ): FwUpdateCheckResult = coroutineScope {
        val resultAsync = async(start = CoroutineStart.UNDISPATCHED) {
            state
                .drop(if (skipsCurrentState) 1 else 0)
                .mapNotNull(::toCheckResult)
                .first()
        }
        try {
            val startResult = startCheck()
            if (startResult is CResult.Failure) {
                error(startResult.error) { "#await could not start update check" }
                return@coroutineScope FwUpdateCheckResult.FAILED
            }
            withTimeoutOrNull(RESULT_TIMEOUT) { resultAsync.await() }
                ?: FwUpdateCheckResult.FAILED.also { info { "#await no result in $RESULT_TIMEOUT" } }
        } finally {
            resultAsync.cancel()
        }
    }

    private fun toCheckResult(state: FwUpdateState): FwUpdateCheckResult? {
        return when (state) {
            FwUpdateState.NoUpdateAvailable -> FwUpdateCheckResult.NO_UPDATE_AVAILABLE
            FwUpdateState.UpdateAvailable -> FwUpdateCheckResult.UPDATE_AVAILABLE
            FwUpdateState.BatteryLow -> FwUpdateCheckResult.BATTERY_LOW
            FwUpdateState.CouldNotCheckUpdate -> FwUpdateCheckResult.FAILED

            FwUpdateState.Pending,
            FwUpdateState.CheckingVersion,
            FwUpdateState.Updating,
            FwUpdateState.Preparing,
            FwUpdateState.DownloadFailure,
            is FwUpdateState.Uploading,
            is FwUpdateState.Downloading -> null
        }
    }
}
