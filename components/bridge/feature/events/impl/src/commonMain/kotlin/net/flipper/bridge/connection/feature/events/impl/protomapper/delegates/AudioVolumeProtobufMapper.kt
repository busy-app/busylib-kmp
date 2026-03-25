package net.flipper.bridge.connection.feature.events.impl.protomapper.delegates

import BSB_State.AudioVolume
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.Fraction

object AudioVolumeProtobufMapper {
    fun map(audioVolume: AudioVolume): BusyLibUpdateEvent.Volume {
        return BusyLibUpdateEvent.Volume(
            AudioVolumeInfo(
                volume = Fraction.fromWholePercent(audioVolume.volume)
            )
        )
    }
}
