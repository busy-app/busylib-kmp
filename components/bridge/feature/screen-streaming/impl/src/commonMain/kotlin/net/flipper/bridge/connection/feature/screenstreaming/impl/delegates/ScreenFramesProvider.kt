package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose

private const val BLK_SIZE = 3

class ScreenFramesProvider(
    private val screenFlow: Flow<BusyLibUpdateEvent.Frame>
) : LogTagProvider {
    override val TAG = "ScreenFramesProvider"
    fun getScreens(): Flow<BusyImageFormat> {
        return screenFlow
            .onEach { verbose { "Receive event: $it" } }
            .mapNotNull { runCatching { mapFrame(it) }.getOrNull() }
    }

    private fun mapFrame(frame: BusyLibUpdateEvent.Frame): BusyImageFormat? {
        val data = when (frame.encoding) {
            BusyLibUpdateEvent.Frame.Encoding.PLAIN -> frame.data
            BusyLibUpdateEvent.Frame.Encoding.RUN_LENGTH -> rleDecompress(frame.data, BLK_SIZE)
            BusyLibUpdateEvent.Frame.Encoding.DEFLATE,
            BusyLibUpdateEvent.Frame.Encoding.DEFLATE_RUN_LENGTH -> return null
        }

        return BusyImageFormat(data)
    }
}
