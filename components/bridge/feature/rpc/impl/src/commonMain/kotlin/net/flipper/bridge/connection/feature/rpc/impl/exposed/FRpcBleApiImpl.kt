package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBleApi
import net.flipper.bridge.connection.feature.rpc.api.model.BleStatusResponse
import net.flipper.bridge.connection.feature.rpc.impl.util.runSafely

class FRpcBleApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : FRpcBleApi {
    override suspend fun getBleStatus(): Result<BleStatusResponse> {
        return runSafely(dispatcher) {
            httpClient.get("/api/ble/status").body<BleStatusResponse>()
        }
    }
}
