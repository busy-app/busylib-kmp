package com.flipperdevices.bridge.connection.transport.ble.api

import com.flipperdevices.bridge.connection.transport.common.api.FDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import kotlinx.collections.immutable.ImmutableMap
import kotlin.uuid.Uuid

data class FBleDeviceConnectionConfig(
    val macAddress: String,
    val serialConfig: FBleDeviceSerialConfig,
    val metaInfoGattMap: ImmutableMap<TransportMetaInfoKey, GATTCharacteristicAddress>
) : FDeviceConnectionConfig<FBleApi>()

data class FBleDeviceSerialConfig(
    val serialServiceUuid: Uuid,
    val rxServiceCharUuid: Uuid,
    val txServiceCharUuid: Uuid,
)
