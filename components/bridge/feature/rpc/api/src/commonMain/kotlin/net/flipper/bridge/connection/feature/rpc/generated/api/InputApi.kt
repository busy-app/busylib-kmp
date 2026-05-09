package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse

interface InputApi {

    /**
     * Send input event
     */
    suspend fun setInputKey(key: kotlin.String): kotlin.Result<SuccessResponse>
}
