package net.flipper.bridge.connection.feature.rpc.impl.critical

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCode
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.impl.client.FRpcClientModeApiImpl
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider

@Inject
@Suppress("TooManyFunctions")
class FRpcCriticalFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    @Assisted private val client: HttpClient,
) : FRpcCriticalFeatureApi, LogTagProvider {
    override val TAG = "FRpcCriticalFeatureApi"
    override val clientModeApi: FRpcClientModeApi = FRpcClientModeApiImpl()
    private val dispatcher = FlipperDispatchers.default

    override suspend fun invalidateLinkedUser(email: String?): Result<RpcLinkedAccountInfo> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                client.get("/api/account/info").body<RpcLinkedAccountInfo>()
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

    override suspend fun deleteAccount(): Result<SuccessResponse> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                client.delete("/api/account").body<SuccessResponse>()
            }
        }
    }

    @Inject
    class InternalFactory(
        private val factory: (HttpClient) -> FRpcCriticalFeatureApiImpl
    ) {
        operator fun invoke(
            client: HttpClient
        ): FRpcCriticalFeatureApiImpl = factory(client)
    }
}
