package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.generated.api.BLEApi
import net.flipper.bridge.connection.feature.rpc.generated.model.BleStatusResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcBleApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
) : BLEApi {
    override suspend fun apiBleStatusGet(): Result<BleStatusResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/ble/status").body<BleStatusResponse>()
        }
    }

    override suspend fun apiBleDisablePost(): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun apiBleEnablePost(): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun apiBlePairingDelete(): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }
}
