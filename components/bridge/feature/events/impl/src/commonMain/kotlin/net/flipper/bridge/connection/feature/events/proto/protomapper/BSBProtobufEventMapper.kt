package net.flipper.bridge.connection.feature.events.proto.protomapper

import BSB_State.StateUpdate
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.AudioVolumeProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.AutoUpdateStateProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.BleProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.BrightnessProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.DeviceNameProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.FrameProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.InputProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.MatterProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.PowerProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.ProfilesProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.TimerProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.TimezoneProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.UpdateCheckProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.UpdateStateProtobufMapper
import net.flipper.bridge.connection.feature.events.proto.protomapper.delegates.WifiProtobufMapper

object BSBProtobufEventMapper {
    @Suppress("CyclomaticComplexMethod")
    fun map(state: StateUpdate): BusyLibUpdateEvent? {
        state.device_name?.let { return DeviceNameProtobufMapper.map(it) }
        state.power?.let { return PowerProtobufMapper.map(it) }
        state.brightness?.let { return BrightnessProtobufMapper.map(it) }
        state.audio_volume?.let { return AudioVolumeProtobufMapper.map(it) }
        state.wifi?.let { return WifiProtobufMapper.map(it) }
        state.update_state?.let { return UpdateStateProtobufMapper.map(it) }
        state.update_check?.let { return UpdateCheckProtobufMapper.map(it) }
        state.timezone?.let { return TimezoneProtobufMapper.map(it) }
        state.matter?.let { return MatterProtobufMapper.map(it) }
        state.frame?.let { return FrameProtobufMapper.map(it) }
        state.input?.let { return InputProtobufMapper.map(it) }
        state.timer?.let { return TimerProtobufMapper.map(it) }
        state.timer_profiles?.let { return ProfilesProtobufMapper.map(it) }
        state.ble?.let { return BleProtobufMapper.map(it) }
        state.auto_update_state?.let { return AutoUpdateStateProtobufMapper.map(it) }
        return null
    }
}
