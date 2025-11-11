package net.flipper.bridge.connection.transport.ble.api

import kotlinx.collections.immutable.ImmutableMap
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
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
