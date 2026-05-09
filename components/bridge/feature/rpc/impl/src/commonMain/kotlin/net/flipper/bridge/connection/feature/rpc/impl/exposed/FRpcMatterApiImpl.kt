package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.generated.api.SmartHomeApi
import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomePairingInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomePairingPayload
import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomeSwitchState
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcMatterApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
) : SmartHomeApi {
    override suspend fun startSmartHomePairing(): Result<SmartHomePairingPayload> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/smart_home/pairing").body<SmartHomePairingPayload>()
        }
    }

    override suspend fun apiSmartHomePairingDelete(): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.delete("/api/smart_home/pairing").body<SuccessResponse>()
        }
    }

    override suspend fun apiSmartHomeSwitchGet(): Result<SmartHomeSwitchState> {
        TODO("Not yet implemented")
    }

    override suspend fun apiSmartHomeSwitchPost(smartHomeSwitchState: SmartHomeSwitchState): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun getSmartHomeCommissioningStatus(): Result<SmartHomePairingInfo> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/smart_home/pairing").body<SmartHomePairingInfo>()
        }
    }
}
