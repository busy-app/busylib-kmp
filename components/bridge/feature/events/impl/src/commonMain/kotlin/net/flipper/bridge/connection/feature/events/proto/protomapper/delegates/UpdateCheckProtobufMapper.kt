package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Update.CheckState
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object UpdateCheckProtobufMapper {
    fun map(checkState: CheckState): BusyLibUpdateEvent.Update.UpdateCheck {
        return BusyLibUpdateEvent.Update.UpdateCheck(
            availableVersion = checkState.available?.version
        )
    }
}
