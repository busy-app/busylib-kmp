package net.flipper.bridge.connection.feature.rpc.api.critical

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCode
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import kotlin.uuid.Uuid

interface FRpcCriticalFeatureApi : FDeviceFeatureApi {
    val clientModeApi: FRpcClientModeApi

    suspend fun invalidateLinkedUser(userId: Uuid?): Result<RpcLinkedAccountInfo>

    suspend fun getLinkCode(): Result<BusyBarLinkCode>

    suspend fun deleteAccount(): Result<SuccessResponse>
}
