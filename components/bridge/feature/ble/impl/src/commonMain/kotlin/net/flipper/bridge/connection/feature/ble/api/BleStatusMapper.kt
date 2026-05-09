package net.flipper.bridge.connection.feature.ble.api

import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.rpc.generated.model.BleStatusResponse

fun BleStatusResponse.toEvent(): BusyLibUpdateEvent.Ble {
    return BusyLibUpdateEvent.Ble(
        status = when (status) {
            BleStatusResponse.Status.RESET -> BusyLibUpdateEvent.Ble.BleServiceStatus.RESET
            BleStatusResponse.Status.INITIALIZATION -> BusyLibUpdateEvent.Ble.BleServiceStatus.INITIALIZATION
            BleStatusResponse.Status.DISABLED -> BusyLibUpdateEvent.Ble.BleServiceStatus.READY
            BleStatusResponse.Status.ENABLED -> BusyLibUpdateEvent.Ble.BleServiceStatus.ADVERTISING
            BleStatusResponse.Status.CONNECTABLE -> BusyLibUpdateEvent.Ble.BleServiceStatus.CONNECTABLE
            BleStatusResponse.Status.CONNECTED -> BusyLibUpdateEvent.Ble.BleServiceStatus.CONNECTED
            BleStatusResponse.Status.INTERNAL_ERROR -> BusyLibUpdateEvent.Ble.BleServiceStatus.ERROR
        },
        remoteAddress = address,
    )
}

fun BusyLibUpdateEvent.Ble.toPublic(): FBleStatus {
    return when (status) {
        BusyLibUpdateEvent.Ble.BleServiceStatus.RESET -> FBleStatus.Reset
        BusyLibUpdateEvent.Ble.BleServiceStatus.INITIALIZATION -> FBleStatus.Initialization
        BusyLibUpdateEvent.Ble.BleServiceStatus.READY -> FBleStatus.Disabled
        BusyLibUpdateEvent.Ble.BleServiceStatus.ADVERTISING -> FBleStatus.Enabled
        BusyLibUpdateEvent.Ble.BleServiceStatus.CONNECTABLE -> FBleStatus.Connectable
        BusyLibUpdateEvent.Ble.BleServiceStatus.CONNECTED -> FBleStatus.Connected(
            address = remoteAddress ?: return FBleStatus.Enabled
        )
        BusyLibUpdateEvent.Ble.BleServiceStatus.ERROR -> FBleStatus.InternalError
    }
}
