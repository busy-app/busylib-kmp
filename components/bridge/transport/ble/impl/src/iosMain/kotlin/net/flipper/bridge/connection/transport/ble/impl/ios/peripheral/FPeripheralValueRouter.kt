package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.ktx.common.toByteArray
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.warn
import platform.CoreBluetooth.CBCharacteristic
import platform.Foundation.NSError
import kotlin.uuid.Uuid

internal class FPeripheralValueRouter(
    private val config: FBleDeviceConnectionConfig,
    private val stateStream: MutableStateFlow<FPeripheralState>,
    private val rxDataChannel: Channel<ByteArray>,
    private val streamingDataChannel: Channel<ByteArray>,
    private val metaInfoKeysStream: MutableStateFlow<Map<TransportMetaInfoKey, ByteArray?>>,
    private val characteristicValueState: MutableStateFlow<Map<Uuid, ByteArray?>>,
    private val gattIO: FPeripheralGattIO,
    private val onError: (NSError) -> Unit,
    private val identifierProvider: () -> String,
) : LogTagProvider {

    override val TAG: String = "FPeripheralValueRouter"

    fun didUpdateValue(
        characteristic: CBCharacteristic,
        error: NSError?
    ) {
        val characteristicUUID = characteristic.UUID.toKotlinUUID()
        val data = characteristic.value
        val payload = data?.toByteArray()

        debug {
            "didUpdateValue uuid=$characteristicUUID bytes=${payload?.size ?: 0} " +
                "hasData=${data != null} error=${error?.localizedDescription} id=${identifierProvider()}"
        }
        if (error != null) {
            gattIO.failRead(characteristicUUID, error)
            error { "#didUpdateValue failed ${error.localizedDescription}" }
            onError(error)
            return
        }

        runBlocking {
            if (characteristicUUID == config.serialConfig.rxServiceCharUuid) {
                if (data != null && payload != null) {
                    rxDataChannel.send(payload)
                    debug { "RX data chunk bytes=${data.length} id=${identifierProvider()}" }
                } else {
                    warn { "RX data is null id=${identifierProvider()}" }
                }
                return@runBlocking
            }

            if (characteristicUUID == config.screenStreamingConfig.notifyCharUuid) {
                if (data != null && payload != null) {
                    streamingDataChannel.send(payload)
                    debug { "Streaming data chunk bytes=${data.length} id=${identifierProvider()}" }
                } else {
                    warn { "Streaming data is null id=${identifierProvider()}" }
                }
                return@runBlocking
            }

            characteristicValueState.update { state ->
                val newMap = state.toMutableMap()
                newMap[characteristicUUID] = payload
                newMap
            }
            debug {
                "Routing characteristic update uuid=$characteristicUUID bytes=${payload?.size ?: 0} " +
                    "id=${identifierProvider()}"
            }
            if (payload != null) {
                gattIO.completeRead(characteristicUUID, payload)
            } else {
                warn { "Characteristic update payload is null uuid=$characteristicUUID id=${identifierProvider()}" }
                return@runBlocking
            }

            val metaKey = config.metaInfoGattMap.entries.firstOrNull { (_, address) ->
                address.characteristicAddress == characteristicUUID
            }?.key

            if (metaKey == null) {
                warn { "Unknown meta characteristic updated: $characteristicUUID" }
            } else {
                stateStream.emit(FPeripheralState.CONNECTED)
                updateMetaInfo(key = metaKey, data = payload)
            }
        }
    }

    private fun updateMetaInfo(key: TransportMetaInfoKey, data: ByteArray) {
        debug { "Update meta info key=$key content ${data.decodeToString()}" }

        metaInfoKeysStream.update {
            val newMap = it.toMutableMap()
            newMap[key] = data
            newMap
        }
    }
}
