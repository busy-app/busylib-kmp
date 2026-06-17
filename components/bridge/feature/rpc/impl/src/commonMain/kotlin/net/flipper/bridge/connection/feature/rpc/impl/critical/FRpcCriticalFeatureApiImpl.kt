package net.flipper.bridge.connection.feature.rpc.impl.critical

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BsbRpcError
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCode
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCodeAlreadyLinked
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCodeResponse
import net.flipper.bridge.connection.feature.rpc.api.model.ErrorResponse
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.impl.client.FRpcClientModeApiImpl
import net.flipper.bridge.connection.transport.common.api.serial.attributes.requireLocalConnection
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.mapSuspendCatching
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import kotlin.uuid.Uuid

@AssistedInject
@Suppress("TooManyFunctions")
class FRpcCriticalFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    @Assisted private val client: HttpClient,
) : FRpcCriticalFeatureApi, LogTagProvider {
    override val TAG = "FRpcCriticalFeatureApi"
    override val clientModeApi: FRpcClientModeApi = FRpcClientModeApiImpl()
    private val _currentAccountInfo = MutableStateFlow<RpcLinkedAccountInfo?>(null)
    override val currentAccountInfo = _currentAccountInfo.asStateFlow()

    private val dispatcher = FlipperDispatchers.default

    override suspend fun invalidateLinkedUser(userId: Uuid?): Result<RpcLinkedAccountInfo> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                client.get("/api/account/info").body<RpcLinkedAccountInfo>()
            }.onSuccess {
                _currentAccountInfo.emit(it)
            }.onSuccess { response -> clientModeApi.updateClientMode(response, userId) }
        }
    }

    override suspend fun getLinkCode(): Result<BusyBarLinkCodeResponse> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                client.post("/api/account/link") {
                    requireLocalConnection()
                }
            }.mapSuspendCatching { call ->
                try {
                    call.body<BusyBarLinkCode>()
                } catch (e: JsonConvertException) {
                    val error = call.body<ErrorResponse>().error
                    if (error == BsbRpcError.ALREADY_LINKED.error) {
                        BusyBarLinkCodeAlreadyLinked
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    override suspend fun deleteAccount(): Result<SuccessResponse> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                client.delete("/api/account") {
                    requireLocalConnection()
                }.body<SuccessResponse>()
            }
        }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            client: HttpClient
        ): FRpcCriticalFeatureApiImpl
    }
}
