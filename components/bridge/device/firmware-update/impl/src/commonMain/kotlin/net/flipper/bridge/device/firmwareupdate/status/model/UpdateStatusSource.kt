package net.flipper.bridge.device.firmwareupdate.status.model

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus

sealed interface UpdateStatusSource {
    val freshUpdateStatus: BsbUpdateStatus?

    data class Fresh(
        override val freshUpdateStatus: BsbUpdateStatus?
    ) : UpdateStatusSource

    data class Cached(
        val cachedUpdateStatus: BsbUpdateStatus,
        override val freshUpdateStatus: BsbUpdateStatus?
    ) : UpdateStatusSource
}
