package net.flipper.bridge.connection.feature.events.impl.protomapper.delegates

import BSB_Frame.Frame
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object FrameProtobufMapper {
    fun map(frame: Frame): BusyLibUpdateEvent.Frame {
        return BusyLibUpdateEvent.Frame(
            width = frame.width,
            height = frame.height,
            data = frame.data_.toByteArray(),
        )
    }
}
