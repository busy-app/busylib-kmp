package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat

interface ScreenFramesProvider {
    fun getScreens(): Flow<BusyImageFormat>
}
