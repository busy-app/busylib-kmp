package net.flipper.bridge.connection.feature.events.model

import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightnessInfo

/**
 * This update events consumed/received only through BusyLib
 */
sealed interface BusyLibUpdateEvent : UpdateEvent {
    data class Brightness(val bsbBrightnessInfo: BsbBrightnessInfo) : BusyLibUpdateEvent

    data class Volume(val audioVolumeInfo: AudioVolumeInfo) : BusyLibUpdateEvent
    data class DeviceName(val deviceName: String) : BusyLibUpdateEvent
}
