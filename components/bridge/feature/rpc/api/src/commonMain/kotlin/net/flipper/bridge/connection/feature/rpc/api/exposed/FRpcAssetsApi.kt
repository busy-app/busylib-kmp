package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse

interface FRpcAssetsApi {
    suspend fun uploadAsset(
        appId: String,
        file: String,
        content: ByteArray
    ): Result<SuccessResponse>

    suspend fun displayDraw(request: DrawRequest): Result<SuccessResponse>
    suspend fun removeDraw(appId: String): Result<SuccessResponse>
}
