package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Update.AutoUpdateState
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object AutoUpdateStateProtobufMapper {
    fun map(autoUpdateState: AutoUpdateState): BusyLibUpdateEvent.AutoUpdateChanged {
        return BusyLibUpdateEvent.AutoUpdateChanged(
            isEnabled = autoUpdateState.enabled
        )
    }
}
