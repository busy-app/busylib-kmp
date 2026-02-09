package net.flipper.bridge.connection.feature.timezone.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.TimestampInfo
import net.flipper.bridge.connection.feature.rpc.api.model.TimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.TimezoneListResponse
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FTimeZoneFeatureApi : FDeviceFeatureApi {
    fun getTimestampInfoFlow(): WrappedFlow<TimestampInfo>
    suspend fun setTimestamp(timestampInfo: TimestampInfo): CResult<Unit>
    fun getTimeZoneInfoFlow(): WrappedFlow<TimezoneInfo>
    suspend fun setTimezone(timezoneInfo: TimezoneInfo): CResult<Unit>
    suspend fun getTimezones(): CResult<TimezoneListResponse>
}
