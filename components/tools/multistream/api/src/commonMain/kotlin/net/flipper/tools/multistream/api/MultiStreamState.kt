package net.flipper.tools.multistream.api

import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat

sealed interface MultiStreamState {
    data object Empty : MultiStreamState
    data class Frame(
        val image: BusyImageFormat
    ) : MultiStreamState
}
