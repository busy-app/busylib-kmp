package com.flipperdevices.bridge.connection.feature.rpc.api.critical

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo

interface FRpcCriticalFeatureApi : FDeviceFeatureApi {
    val clientModeApi: FRpcClientModeApi

    suspend fun checkLinkedUser(userId: String?): Result<RpcLinkedAccountInfo>
}
