package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat

private const val BLK_SIZE = 3

class WebSocketScreenFramesProvider(
    private val rpcFeatureApi: FRpcFeatureApi
) : ScreenFramesProvider {
    override fun getScreens(): Flow<BusyImageFormat> {
        return rpcFeatureApi.fRpcWebSocketApi
            .getScreenFrames()
            .map { rleDecompress(it, BLK_SIZE) }
            .map(::BusyImageFormat)
    }
}

/**
 * Decompresses run-length encoded data.
 *
 * Control byte format:
 * - High bit set (0x80): unique blocks follow, lower 7 bits = count of unique blocks
 * - High bit clear: repeated block, byte value = repeat count
 */
@Suppress("MagicNumber")
fun rleDecompress(data: ByteArray, blkSize: Int): ByteArray {
    var index = 0
    val dataLen = data.size
    val decompressed = mutableListOf<Byte>()

    while (index < dataLen) {
        val ctrlByte = data[index].toInt() and 0xFF
        index++

        if ((ctrlByte and 0x80) != 0) {
            // Unique blocks: ctrl_byte & 0x7F = unique sequence length
            val count = ctrlByte and 0x7F
            for (i in 0 until count * blkSize) {
                decompressed.add(data[index + i])
            }
            index += count * blkSize
        } else {
            // Repeated block: ctrl_byte = repeat count
            val count = ctrlByte
            val block = data.copyOfRange(index, index + blkSize)
            repeat(count) {
                for (j in 0 until blkSize) {
                    decompressed.add(block[j])
                }
            }
            index += blkSize
        }
    }

    return decompressed.toByteArray()
}
