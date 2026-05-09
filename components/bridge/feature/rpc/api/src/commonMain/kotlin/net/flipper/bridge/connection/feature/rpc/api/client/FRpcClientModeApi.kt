package net.flipper.bridge.connection.feature.rpc.api.client

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.rpc.generated.model.AccountInfo
import kotlin.uuid.Uuid

interface FRpcClientModeApi {
    enum class HttpClientMode {
        CRITICAL, DEFAULT
    }

    val httpClientModeFlow: StateFlow<HttpClientMode>

    fun updateClientMode(info: AccountInfo, userId: Uuid?)
}
