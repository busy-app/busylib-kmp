package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.BleStatusResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse

interface BLEApi {

    /**
     * Disable BLE
     */
    suspend fun apiBleDisablePost(): kotlin.Result<SuccessResponse>

    /**
     * Enable BLE
     */
    suspend fun apiBleEnablePost(): kotlin.Result<SuccessResponse>

    /**
     * Remove pairing
     */
    suspend fun apiBlePairingDelete(): kotlin.Result<SuccessResponse>

    /**
     */
    suspend fun apiBleStatusGet(): kotlin.Result<BleStatusResponse>
}
