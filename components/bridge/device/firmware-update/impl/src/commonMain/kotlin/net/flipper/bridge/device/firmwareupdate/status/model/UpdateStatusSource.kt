package net.flipper.bridge.device.firmwareupdate.status.model

import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.DeviceUpdateStatus
import net.flipper.bridge.device.firmwareupdate.status.api.UpdateStatusProvider

/**
 * When receiving a status via [FFirmwareUpdateFeatureApi.updateStatusFlow] we keep both the current and previous state.
 * Initially we have [UpdateStatusSource.Fresh] with a `null` status — no device has reported anything yet.
 *
 * After the device reboots, the last received status is cached in [UpdateStatusSource.Cached.status],
 * and the fresh status is reset (no longer available).
 *
 * We know that the device is updating when we have a cached status but no fresh status.
 * The [DeviceUpdateStatus] payload carries the id of the device the status belongs to.
 * @see UpdateStatusProvider
 */
sealed interface UpdateStatusSource {
    val status: DeviceUpdateStatus?

    data class Fresh(override val status: DeviceUpdateStatus?) : UpdateStatusSource

    data class Cached(override val status: DeviceUpdateStatus) : UpdateStatusSource
}
