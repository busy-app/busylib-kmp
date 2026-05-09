package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse

interface BusyApi {

    /**
     * Get BUSY timer profile
     */
    suspend fun getBusyProfile(slot: String): kotlin.Result<String>

    /**
     * Get BUSY timer snapshot
     */
    suspend fun getBusySnapshot(): kotlin.Result<String>

    /**
     * Set BUSY time profile
     */
    suspend fun setBusyProfile(slot: String, rawJson: String): kotlin.Result<SuccessResponse>

    /**
     * Set BUSY timer snapshot
     */
    suspend fun setBusySnapshot(rawJson: String): kotlin.Result<SuccessResponse>
}
