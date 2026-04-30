package net.flipper.bridge.connection.feature.timezone.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.timezone.currentTimeZoneAbbreviation
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

private val DELAY = 10.seconds

class AutoProvisioningTimeZone(
    private val timeZoneFeature: FTimeZoneFeatureApi
) : FOnDeviceReadyFeatureApi, LogTagProvider {
    override val TAG = "AutoProvisioningTimeZone"

    override suspend fun onReady() {
        info { "Started, but delayed for $DELAY..." }
        delay(DELAY)
        val activeTimeZone = timeZoneFeature.getTimeZoneInfoFlow()
            .first()
        debug { "Receive timezone $activeTimeZone" }
        if (isCurrentTimeZone(activeTimeZone)) {
            info { "Found same timezone, skip" }
            return
        }

        val allTimeZones = timeZoneFeature.getTimezones()
            .onFailure {
                error(it) { "Failed to receive all timezones" }
            }.getOrNull() ?: return
        val newTimeZone = findClosestTimeZone(allTimeZones)
        timeZoneFeature.setTimezone(newTimeZone)
            .onFailure {
                error(it) { "Failed setup timezone" }
            }.onSuccess {
                info { "Successfully setup new timezone: $newTimeZone" }
            }
    }

    private fun findClosestTimeZone(timeZones: List<TimezoneInfo>): TimezoneInfo {
        val currentTz = TimeZone.currentSystemDefault()
        val currentAbbr = currentTimeZoneAbbreviation()
        val currentTzCity = currentTz.id.substringAfterLast('/')
        info { "Current timezone abbreviation: $currentAbbr, id: ${currentTz.id}" }

        val abbrMatches = timeZones.filter { it.abbr == currentAbbr }
        if (abbrMatches.size == 1) {
            info { "Found by direct abbreviation match" }
            return abbrMatches.first()
        }
        if (abbrMatches.size > 1) {
            abbrMatches.find { it.name.equals(currentTzCity, ignoreCase = true) }
                ?.let {
                    info { "Find by city and abbreviation match" }
                    return it
                }
            info { "Fallback to first match from abbreviation matches" }
            return abbrMatches.first()
        }

        timeZones.find { it.name.equals(currentTzCity, ignoreCase = true) }
            ?.let {
                info { "Find by city match" }
                return it
            }

        val currentOffsetSeconds = currentTz
            .offsetAt(Clock.System.now())
            .totalSeconds
        return timeZones.minBy {
            abs(UtcOffset.parse(it.offset).totalSeconds - currentOffsetSeconds)
        }
    }

    private fun isCurrentTimeZone(timeZone: TimezoneInfo): Boolean {
        return timeZone.abbr == currentTimeZoneAbbreviation()
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
