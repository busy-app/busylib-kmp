package net.flipper.bridge.connection.feature.events.impl.protomapper.delegates

import BSB_Timer.Timer
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object TimerProtobufMapper {
    fun map(timer: Timer): BusyLibUpdateEvent.Timer? {
        val json = timer.json ?: return null
        return BusyLibUpdateEvent.Timer(
            json = json.data_.toByteArray().decodeToString(),
        )
    }
}
