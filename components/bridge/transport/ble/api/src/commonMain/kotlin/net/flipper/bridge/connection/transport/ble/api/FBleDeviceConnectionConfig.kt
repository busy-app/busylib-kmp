package net.flipper.bridge.connection.transport.ble.api

import kotlinx.collections.immutable.ImmutableMap
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.data.nonEmptyListOf
import kotlin.uuid.Uuid

data class FBleDeviceConnectionConfig(
    val deviceName: String,
    val macAddress: String,
    val serialConfig: FBleDeviceSerialConfig,
    val metaInfoGattMap: ImmutableMap<TransportMetaInfoKey, GATTCharacteristicAddress>
) : FDeviceConnectionConfig<FBleApi>() {
    override fun getTransportTypes() = nonEmptyListOf(FInternalTransportConnectionType.BLE)
}

data class FBleDeviceSerialConfig(
    val serialServiceUuid: Uuid,
    val rxServiceCharUuid: Uuid,
    val txServiceCharUuid: Uuid,
    val resetCharUuid: Uuid
)
