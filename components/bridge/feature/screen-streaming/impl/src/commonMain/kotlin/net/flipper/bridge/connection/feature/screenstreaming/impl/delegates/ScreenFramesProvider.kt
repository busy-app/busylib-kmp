package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat

interface ScreenFramesProvider {
    suspend fun getScreens(): Flow<BusyImageFormat>
}
