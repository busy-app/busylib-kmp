package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralGattIO
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralState
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.ktx.common.toByteArray
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.warn
import net.flipper.core.busylib.systrace.NativeTracer
import net.flipper.core.busylib.systrace.trace
import platform.Foundation.NSData
import kotlin.uuid.Uuid

private class BLEEvent(
    val characteristicUUID: Uuid,
    val data: NSData?,
)

private const val BUFFER_SIZE = 1024

internal class BLEEventQueue(
    private val config: FBleDeviceConnectionConfig,
    private val stateStream: MutableStateFlow<FPeripheralState>,
    private val rxDataChannel: Channel<ByteArray>,
    private val streamingDataChannel: Channel<ByteArray>,
    private val metaInfoKeysStream: MutableStateFlow<Map<TransportMetaInfoKey, ByteArray?>>,
    private val characteristicValueState: MutableStateFlow<Map<Uuid, ByteArray?>>,
    private val gattIO: FPeripheralGattIO,
    private val identifierProvider: () -> String,
    private val scope: CoroutineScope
) : LogTagProvider {
    override val TAG = "ProcessBLEEventQueue"

    private val queue = MeteredChannel<BLEEvent>(
        BUFFER_SIZE
    )

    fun onProcess(
        characteristicUUID: Uuid,
        data: NSData?,
    ) {
        if (characteristicUUID == config.statusStreamingConfig.notifyCharUuid) {
            if (queue.is75PercentFull) {
                error {
                    "!!! Failed to write streaming ble event because buffer overflow. " +
                        "Current fill ratio: ${queue.fillRatio} !!!"
                }
            } else {
                val isSuccess = queue.trySend(BLEEvent(characteristicUUID, data))
                if (isSuccess.not()) {
                    error { "Failed to write streaming ble event" }
                }
            }
        } else {
            runBlocking {
                if (queue.is75PercentFull) {
                    error { "!!! Trying to write to queue who >75% filled" }
                }
                queue.send(BLEEvent(characteristicUUID, data))
            }
        }
    }

    init {
        scope.launch {
            while (isActive) {
                val event = queue.receive()
                NativeTracer.trace("ble_event") {
                    processEvent(event)
                }
            }
        }
    }

    private suspend fun processEvent(bleEvent: BLEEvent) {
        val characteristicUUID = bleEvent.characteristicUUID
        val data = bleEvent.data
        val payload = data?.toByteArray()

        if (characteristicUUID == config.serialConfig.rxServiceCharUuid) {
            if (data != null && payload != null) {
                rxDataChannel.send(payload)
                debug { "RX data chunk bytes=${data.length} id=${identifierProvider()}" }
            } else {
                warn { "RX data is null id=${identifierProvider()}" }
            }
            return
        }

        if (characteristicUUID == config.statusStreamingConfig.notifyCharUuid) {
            if (data != null && payload != null) {
                streamingDataChannel.send(payload)
                debug { "Streaming data chunk bytes=${data.length} id=${identifierProvider()}" }
            } else {
                warn { "Streaming data is null id=${identifierProvider()}" }
            }
            return
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
            return
        }

        val metaKey = config.metaInfoGattMap.entries.firstOrNull { (_, address) ->
            address.characteristicAddress == characteristicUUID
        }?.key

        if (metaKey != null) {
            stateStream.emit(FPeripheralState.CONNECTED)
            updateMetaInfo(key = metaKey, data = payload)
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
