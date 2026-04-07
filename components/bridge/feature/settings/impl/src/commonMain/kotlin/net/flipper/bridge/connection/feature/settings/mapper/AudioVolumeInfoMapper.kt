package net.flipper.bridge.connection.feature.settings.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.settings.model.BsbVolume

internal fun AudioVolumeInfo.toBsbVolume(): BsbVolume {
    return BsbVolume(volume)
}
