package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import kotlin.uuid.Uuid

interface FPeripheralApi {
    val identifier: NSUUID
    val name: String?
    val stateStream: WrappedStateFlow<FPeripheralState>

    val rxDataStream: Flow<ByteArray>

    val streamingDataStream: Flow<ByteArray>
    val metaInfoKeysStream: WrappedStateFlow<Map<TransportMetaInfoKey, ByteArray?>>

    suspend fun writeValue(data: ByteArray)
    suspend fun readValue(characteristicUuid: Uuid): ByteArray
    suspend fun writeValue(characteristicUuid: Uuid, data: ByteArray)

    suspend fun onConnecting()
    suspend fun onConnect()
    suspend fun onDisconnecting()
    suspend fun onDisconnect()
    fun onError(error: NSError)
}
