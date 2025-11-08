package com.flipperdevices.bridge.connection.feature.rpc.impl.critical

import com.flipperdevices.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarLinkCode
import com.flipperdevices.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import com.flipperdevices.bridge.connection.feature.rpc.impl.client.FRpcClientModeApiImpl
import com.flipperdevices.core.busylib.ktx.common.FlipperDispatchers
import com.flipperdevices.core.busylib.ktx.common.runSuspendCatching
import com.flipperdevices.core.busylib.log.LogTagProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
class FRpcCriticalFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    private val client: HttpClient,
) : FRpcCriticalFeatureApi, LogTagProvider {
    override val TAG = "FRpcCriticalFeatureApi"
    override val clientModeApi: FRpcClientModeApi = FRpcClientModeApiImpl()
    private val dispatcher = FlipperDispatchers.default

    override suspend fun invalidateLinkedUser(email: String?): Result<RpcLinkedAccountInfo> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                client.get("/api/account").body<RpcLinkedAccountInfo>()
            }.onSuccess { response -> clientModeApi.updateClientMode(response, email) }
        }
    }

    override suspend fun getLinkCode(): Result<BusyBarLinkCode> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                client.post("/api/account/link").body<BusyBarLinkCode>()
            }
        }
    }

    fun interface InternalFactory {
        operator fun invoke(
            client: HttpClient
        ): FRpcCriticalFeatureApiImpl
    }
}
