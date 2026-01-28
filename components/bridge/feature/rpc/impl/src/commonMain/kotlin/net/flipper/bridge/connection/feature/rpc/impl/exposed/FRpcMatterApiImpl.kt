package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissioningPayload
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcMatterApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
) : FRpcMatterApi {
    override suspend fun postMatterCommissioning(): Result<MatterCommissioningPayload> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/matter/commissioning").body<MatterCommissioningPayload>()
        }
    }

    override suspend fun deleteMatterCommissioning(): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            httpClient.delete("/api/matter/commissioning").body<MatterCommissioningPayload>()
        }
    }
}
