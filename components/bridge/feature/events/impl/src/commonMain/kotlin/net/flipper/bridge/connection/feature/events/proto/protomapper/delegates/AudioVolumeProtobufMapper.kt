package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.AudioVolume
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.core.busylib.data.Fraction

object AudioVolumeProtobufMapper {
    fun map(audioVolume: AudioVolume): BusyLibUpdateEvent.Volume {
        return BusyLibUpdateEvent.Volume(
            volume = Fraction.fromWholePercent(audioVolume.volume)
        )
    }
}
