package net.flipper.bridge.connection.feature.timezone.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FTimeZoneFeatureApi : FDeviceFeatureApi {
    fun getTimeZoneInfoFlow(): WrappedFlow<TimezoneInfo>
    suspend fun setTimezone(timezoneInfo: TimezoneInfo): CResult<Unit>
    suspend fun getTimezones(): CResult<List<TimezoneInfo>>
}
