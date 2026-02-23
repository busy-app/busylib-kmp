package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose
import kotlin.io.encoding.Base64

private const val KEY_SCREEN = "front_display"

class MetaInfoScreenFramesProvider(
    private val screenFlow: Flow<TransportMetaInfoData?>
) : ScreenFramesProvider, LogTagProvider {
    override val TAG = "MetaInfoScreenFramesProvider"
    override suspend fun getScreens(): Flow<BusyImageFormat> {
        return screenFlow
            .onEach { verbose { "Receive event: $it" } }
            .filterNotNull()
            .filterIsInstance<TransportMetaInfoData.StringValue>()
            .filter { it.key == KEY_SCREEN }
            .mapNotNull { runCatching { Base64.decode(it.value) }.getOrNull() }
            .map { BusyImageFormat(it) }
    }
}
