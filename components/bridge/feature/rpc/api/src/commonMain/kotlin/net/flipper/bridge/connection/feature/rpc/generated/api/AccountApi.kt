package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.AccountInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.AccountLink
import net.flipper.bridge.connection.feature.rpc.generated.model.AccountProfile
import net.flipper.bridge.connection.feature.rpc.generated.model.AccountStatus
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse

interface AccountApi {

    /**
     * Get linked account info
     */
    suspend fun getAccountInfo(): kotlin.Result<AccountInfo>

    /**
     * Get MQTT profile
     */
    suspend fun getAccountProfile(): kotlin.Result<AccountProfile>

    /**
     * Get MQTT status info
     */
    suspend fun getAccountStatus(): kotlin.Result<AccountStatus>

    /**
     * Link device to account
     */
    suspend fun linkAccount(): kotlin.Result<AccountLink>

    /**
     * Set MQTT profile
     */
    suspend fun setAccountProfile(
        profile: kotlin.String,
        customUrl: kotlin.String? = null
    ): kotlin.Result<SuccessResponse>

    /**
     * Unlink device from account
     */
    suspend fun unlinkAccount(): kotlin.Result<SuccessResponse>
}
