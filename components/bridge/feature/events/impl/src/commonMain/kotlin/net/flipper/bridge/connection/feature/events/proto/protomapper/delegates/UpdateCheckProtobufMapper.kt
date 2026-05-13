package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Update.CheckError
import BSB_Update.CheckEvent
import BSB_Update.CheckState
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateCheck
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateCheck.CheckResult

object UpdateCheckProtobufMapper {
    fun map(checkState: CheckState): BusyLibUpdateEvent.Update.UpdateCheck? {
        val available = checkState.available
        val unavailable = checkState.unavailable
        val result = if (available != null) {
            CheckResult.Available(available.version)
        } else if (unavailable != null) {
            CheckResult.Unavailable(
                reason = when (unavailable.reason) {
                    CheckError.FAILURE -> CheckResult.Unavailable.CheckError.FAILURE
                    is CheckError.Unrecognized,
                    CheckError.IDLE -> CheckResult.Unavailable.CheckError.IDLE
                    CheckError.NOT_AVAILABLE -> CheckResult.Unavailable.CheckError.NOT_AVAILABLE
                }
            )
        } else {
            return null
        }
        val event = when (checkState.event) {
            CheckEvent.START -> UpdateCheck.CheckEvent.START
            CheckEvent.STOP -> UpdateCheck.CheckEvent.STOP
            CheckEvent.NONE,
            is CheckEvent.Unrecognized -> UpdateCheck.CheckEvent.NONE
        }

        return UpdateCheck(
            result = result,
            event = event
        )
    }
}
