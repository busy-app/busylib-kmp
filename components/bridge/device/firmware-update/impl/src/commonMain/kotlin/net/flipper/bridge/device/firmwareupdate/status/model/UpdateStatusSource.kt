package net.flipper.bridge.device.firmwareupdate.status.model

import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.device.firmwareupdate.status.api.UpdateStatusProvider

/**
 * When receiving a status via [FFirmwareUpdateFeatureApi.updateStatusFlow] we keep both the current and previous state.
 * Initially we have [UpdateStatusSource.Fresh] with a current status that is null.
 *
 * After the device reboots, the last received status is cached in [UpdateStatusSource.Cached.cachedUpdateStatus],
 * and the fresh status is reset (no longer available).
 *
 * We know that the device is updating when we have a cached status but no fresh status.
 * @see UpdateStatusProvider
 */
sealed interface UpdateStatusSource {

    data class Fresh(val freshUpdateStatus: BsbUpdateStatus?) : UpdateStatusSource

    data class Cached(val cachedUpdateStatus: BsbUpdateStatus) : UpdateStatusSource
}
