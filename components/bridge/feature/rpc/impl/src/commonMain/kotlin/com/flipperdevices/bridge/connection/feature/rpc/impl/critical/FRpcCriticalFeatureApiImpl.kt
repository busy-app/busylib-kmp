package com.flipperdevices.bridge.connection.feature.rpc.impl.critical

import com.flipperdevices.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import com.flipperdevices.bridge.connection.feature.rpc.impl.client.FRpcClientModeApiImpl
import com.flipperdevices.core.ktx.common.FlipperDispatchers
import com.flipperdevices.core.ktx.common.runSuspendCatching
import com.flipperdevices.core.log.LogTagProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.withContext

@Inject
@Suppress("TooManyFunctions")
class FRpcCriticalFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    @Assisted private val client: HttpClient,
) : FRpcCriticalFeatureApi, LogTagProvider {
    override val TAG = "FRpcCriticalFeatureApi"
    override val clientModeApi: FRpcClientModeApi = FRpcClientModeApiImpl()
    private val dispatcher = FlipperDispatchers.default

    override suspend fun checkLinkedUser(userId: String?): Result<RpcLinkedAccountInfo> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                RpcLinkedAccountInfo(RpcLinkedAccountInfo.State.NOT_LINKED)
//                client.get("/api/account").body<RpcLinkedAccountInfo>()
            }.onSuccess { response -> clientModeApi.updateClientMode(response, userId) }
        }
    }

    @AssistedFactory
    fun interface InternalFactory {
        operator fun invoke(
            client: HttpClient
        ): FRpcCriticalFeatureApiImpl
    }
}
