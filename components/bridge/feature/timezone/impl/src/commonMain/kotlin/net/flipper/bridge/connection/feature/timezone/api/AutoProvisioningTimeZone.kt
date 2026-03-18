package net.flipper.bridge.connection.feature.timezone.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneListItem
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

class AutoProvisioningTimeZone(
    private val timeZoneFeature: FTimeZoneFeatureApi
) : FOnDeviceReadyFeatureApi, LogTagProvider {
    override val TAG = "AutoProvisioningTimeZone"

    override suspend fun onReady() {
        val allTimeZones = timeZoneFeature.getTimezones()
            .onFailure {
                error(it) { "Failed to receive all timezones" }
            }.getOrNull() ?: return
        val activeTimeZone = timeZoneFeature.getTimeZoneInfoFlow()
            .first()
        debug { "Receive timezone $activeTimeZone" }
        val activeTimeZoneItem = allTimeZones.find { it.name == activeTimeZone.timezone }
        if (activeTimeZoneItem == null) {
            error { "Failed to find active time zone (${activeTimeZone.timezone}) from list" }
            return
        }
        if (isCurrentTimeZone(activeTimeZoneItem)) {
            info { "Found same timezone, skip" }
            return
        }
        val newTimeZone = findClosestTimeZone(allTimeZones)
        timeZoneFeature.setTimezone(TimezoneInfo(newTimeZone.name))
            .onFailure {
                error(it) { "Failed setup timezone" }
            }.onSuccess {
                info { "Successfully setup new timezone: $newTimeZone" }
            }
    }

    private fun findClosestTimeZone(timeZones: List<TimezoneListItem>): TimezoneListItem {
        val currentTimeZone = TimeZone.currentSystemDefault()

    }

    private fun isCurrentTimeZone(timeZone: TimezoneListItem): Boolean {
        val currentTimeZone = TimeZone.currentSystemDefault()

    }

    @Inject
    @ContributesBinding(
        BusyLibGraph::class,
        FOnDeviceReadyFeatureApi.Factory::class,
        multibinding = true
    )
    class Factory : FOnDeviceReadyFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FOnDeviceReadyFeatureApi? {
            val fTimeZoneFeatureApi = unsafeFeatureDeviceApi
                .get(FTimeZoneFeatureApi::class)
                ?.await() ?: return null

            return AutoProvisioningTimeZone(
                timeZoneFeature = fTimeZoneFeatureApi
            )
        }
    }
}