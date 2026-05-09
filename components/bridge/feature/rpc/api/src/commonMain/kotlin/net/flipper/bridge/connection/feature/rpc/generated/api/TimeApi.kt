package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.TimestampInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.TimezoneInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.TimezoneListResponse

interface TimeApi {

    /**
     * Get current timestamp with timezone
     */
    suspend fun getTime(): kotlin.Result<TimestampInfo>

    /**
     * Get timezone
     */
    suspend fun getTimeTimezone(): kotlin.Result<TimezoneInfo>

    /**
     * Get list of supported time zones
     */
    suspend fun getTimeTzlist(): kotlin.Result<TimezoneListResponse>

    /**
     * Set current timestamp
     */
    suspend fun setTimeTimestamp(timestamp: kotlin.String): kotlin.Result<SuccessResponse>

    /**
     * Set timezone
     */
    suspend fun setTimeTimezone(timezone: kotlin.String): kotlin.Result<SuccessResponse>
}
