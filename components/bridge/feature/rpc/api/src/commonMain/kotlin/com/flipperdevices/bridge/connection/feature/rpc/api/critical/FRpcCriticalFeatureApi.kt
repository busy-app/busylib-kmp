package com.flipperdevices.bridge.connection.feature.rpc.api.critical

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarLinkCode
import com.flipperdevices.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo

interface FRpcCriticalFeatureApi : FDeviceFeatureApi {
    val clientModeApi: FRpcClientModeApi

    suspend fun invalidateLinkedUser(email: String?): Result<RpcLinkedAccountInfo>

    suspend fun getLinkCode(): Result<BusyBarLinkCode>
}
