package net.flipper.bridge.connection.screens.dashboard.timezone

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.timezone.api.FTimeZoneFeatureApi
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel

class TimezoneDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    val timezoneInfoFlow = featureProvider
        .get(FTimeZoneFeatureApi::class)
        .getResource { it.getTimeZoneInfoFlow() }

    private val mutableState = MutableStateFlow(TimezoneDashboardState())
    val state: StateFlow<TimezoneDashboardState> = mutableState

    fun refreshTimezones() = runAction("timezone list") {
        val timezoneApi = requireFeature<FTimeZoneFeatureApi>(featureProvider, "Timezone")
        val timezones = timezoneApi.getTimezones().getOrThrow()
        mutableState.value = mutableState.value.copy(
            lastTimezonesSummary = buildString {
                append("${timezones.size} items")
                timezones.firstOrNull()?.let { first ->
                    append(", first=${first.name} (${first.abbr})")
                }
            }
        )
        appendLog("Timezones received: ${timezones.size}")
    }

    fun setCurrentOrFirstTimezone() = runAction("timezone set") {
        val timezoneApi = requireFeature<FTimeZoneFeatureApi>(featureProvider, "Timezone")
        val allTimezones = timezoneApi.getTimezones().getOrThrow()
        require(allTimezones.isNotEmpty()) { "Timezone list is empty" }
        val activeTimezone = timezoneInfoFlow.value?.name
        val targetTimezone = allTimezones.firstOrNull { it.name == activeTimezone } ?: allTimezones.first()
        timezoneApi.setTimezone(targetTimezone).getOrThrow()
        mutableState.value = mutableState.value.copy(
            lastSetTimezone = "${targetTimezone.name} (${targetTimezone.abbr}, ${targetTimezone.offset})"
        )
        appendLog("Timezone set to ${targetTimezone.name}")
    }
}

data class TimezoneDashboardState(
    val lastTimezonesSummary: String? = null,
    val lastSetTimezone: String? = null
)
