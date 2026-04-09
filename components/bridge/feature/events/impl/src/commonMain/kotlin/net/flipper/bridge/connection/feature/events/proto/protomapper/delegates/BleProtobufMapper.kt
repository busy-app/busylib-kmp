package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.Ble.Ble
import BSB_State.Ble.ServiceStatus
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Ble.BleServiceStatus

object BleProtobufMapper {
    fun map(ble: Ble): BusyLibUpdateEvent.Ble {
        return BusyLibUpdateEvent.Ble(
            status = ble.status.toServiceStatus(),
            remoteAddress = ble.remote_address,
        )
    }

    private fun ServiceStatus.toServiceStatus(): BleServiceStatus {
        return when (this) {
            ServiceStatus.RESET -> BleServiceStatus.RESET
            ServiceStatus.INITIALIZATION -> BleServiceStatus.INITIALIZATION
            ServiceStatus.READY -> BleServiceStatus.READY
            ServiceStatus.ADVERTISING -> BleServiceStatus.ADVERTISING
            ServiceStatus.CONNECTABLE -> BleServiceStatus.CONNECTABLE
            ServiceStatus.CONNECTED -> BleServiceStatus.CONNECTED
            ServiceStatus.ERROR -> BleServiceStatus.ERROR
            is ServiceStatus.Unrecognized -> BleServiceStatus.RESET
        }
    }
}
