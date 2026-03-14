package net.flipper.bridge.device.firmwareupdate.status.model

import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus

sealed interface UpdateStatusSource {
    val freshUpdateStatus: UpdateStatus?

    data class Fresh(
        override val freshUpdateStatus: UpdateStatus?
    ) : UpdateStatusSource

    data class Cached(
        val cachedUpdateStatus: UpdateStatus,
        override val freshUpdateStatus: UpdateStatus?
    ) : UpdateStatusSource
}
