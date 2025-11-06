package com.flipperdevices.bridge.api.scanner

import no.nordicsemi.kotlin.ble.client.android.Peripheral
import kotlin.uuid.Uuid

sealed interface DiscoveredBluetoothDevice {
    // Wrapper for data variables
    val address: String
    val name: String?
    val services: List<Uuid>

    interface RealDiscoveredBluetoothDevice : DiscoveredBluetoothDevice {
        val device: Peripheral
    }

    data object MockDiscoveredBluetoothDevice : DiscoveredBluetoothDevice {
        override val address: String = "busy_bar_mock"
        override val name = "BUSY Bar Mock"
        override val services: List<Uuid> = emptyList()
    }
}
