package net.flipper.bridge.connection.feature.rpc.api.critical

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCode
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo

interface FRpcCriticalFeatureApi : FDeviceFeatureApi {
    val clientModeApi: FRpcClientModeApi

    suspend fun invalidateLinkedUser(email: String?): Result<RpcLinkedAccountInfo>

    suspend fun getLinkCode(): Result<BusyBarLinkCode>
}
