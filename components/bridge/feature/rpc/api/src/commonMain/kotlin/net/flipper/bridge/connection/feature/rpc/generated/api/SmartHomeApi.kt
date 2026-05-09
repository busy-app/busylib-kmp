package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomePairingInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomePairingPayload
import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomeSwitchState
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse

interface SmartHomeApi {

    /**
     * Erase all smart home links
     */
    suspend fun apiSmartHomePairingDelete(): kotlin.Result<SuccessResponse>

    /**
     * Get state of emulated smart home switch
     */
    suspend fun apiSmartHomeSwitchGet(): kotlin.Result<SmartHomeSwitchState>

    /**
     * Set state of emulated smart home switch
     */
    suspend fun apiSmartHomeSwitchPost(smartHomeSwitchState: SmartHomeSwitchState): kotlin.Result<SuccessResponse>

    /**
     * Smart home commissioning status
     */
    suspend fun getSmartHomeCommissioningStatus(): kotlin.Result<SmartHomePairingInfo>

    /**
     * Link device to a smart home
     */
    suspend fun startSmartHomePairing(): kotlin.Result<SmartHomePairingPayload>
}
