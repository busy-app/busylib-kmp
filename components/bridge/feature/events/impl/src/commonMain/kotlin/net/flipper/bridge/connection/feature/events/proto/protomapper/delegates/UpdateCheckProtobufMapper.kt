package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Update.CheckState
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object UpdateCheckProtobufMapper {
    fun map(checkState: CheckState): BusyLibUpdateEvent.UpdateCheck {
        return BusyLibUpdateEvent.UpdateCheck(
            availableVersion = checkState.available?.version
        )
    }
}
