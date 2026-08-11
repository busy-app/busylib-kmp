package net.flipper.bridge.connection.feature.drawtool.impl

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse

/**
 * [FRpcAssetsApi] whose every call outcome is dictated by the test.
 */
class FakeFRpcAssetsApi(
    private val uploadResult: Result<SuccessResponse> = Result.success(SuccessResponse("ok")),
    private val displayDrawResult: Result<SuccessResponse> = Result.success(SuccessResponse("ok")),
    private val removeDrawResult: Result<SuccessResponse> = Result.success(SuccessResponse("ok"))
) : FRpcAssetsApi {
    var displayDrawRequests: List<DrawRequest> = emptyList()
        private set

    override suspend fun uploadAsset(
        appId: String,
        file: String,
        content: ByteArray
    ): Result<SuccessResponse> = uploadResult

    override suspend fun displayDraw(request: DrawRequest): Result<SuccessResponse> {
        displayDrawRequests = displayDrawRequests + request
        return displayDrawResult
    }

    override suspend fun removeDraw(appId: String): Result<SuccessResponse> = removeDrawResult
}
