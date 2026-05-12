package net.flipper.bridge.device.firmwareupdate.status.model

import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.device.firmwareupdate.status.api.UpdateStatusProvider


/**
 * При получении статуса через [FFirmwareUpdateFeatureApi.updateStatusFlow] мы храним текущее и предыдущее состояние
 * Изначально у нас имеется [UpdateStatusSource.Fresh] только с текущим статусом, который null
 *
 * После ребута устройства последний полученный статус кешируем в [UpdateStatusSource.Cached.cachedUpdateStatus], а [UpdateStatusSource.Cached.freshUpdateStatus] соответственно становится null
 *
 * Мо понимае, что деввайс апдейтитс, если у нас есть закешированный статус и отсутствует текущий статус
 * @see UpdateStatusProvider
 */
sealed interface UpdateStatusSource {

    data class Fresh(val freshUpdateStatus: BsbUpdateStatus?) : UpdateStatusSource

    data class Cached(val cachedUpdateStatus: BsbUpdateStatus) : UpdateStatusSource
}
