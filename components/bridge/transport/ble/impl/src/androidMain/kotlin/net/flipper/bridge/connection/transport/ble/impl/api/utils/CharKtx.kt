package net.flipper.bridge.connection.transport.ble.impl.api.utils

import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty

fun RemoteCharacteristic.isNotifyAvailable(): Boolean {
    return properties
        .intersect(listOf(CharacteristicProperty.NOTIFY, CharacteristicProperty.INDICATE))
        .isNotEmpty()
}
