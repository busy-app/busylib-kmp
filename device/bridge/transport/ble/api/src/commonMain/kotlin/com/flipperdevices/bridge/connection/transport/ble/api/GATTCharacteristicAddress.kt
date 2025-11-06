package com.flipperdevices.bridge.connection.transport.ble.api

import kotlin.uuid.Uuid

data class GATTCharacteristicAddress(
    val serviceAddress: Uuid,
    val characteristicAddress: Uuid
)
