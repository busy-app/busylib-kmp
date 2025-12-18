package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatus
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusPower
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.feature.rpc.impl.util.runSafely

class FRpcSystemApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : FRpcSystemApi {
    override suspend fun getVersion(): Result<BusyBarVersion> {
        return runSafely(dispatcher) {
            httpClient.get("/api/version").body<BusyBarVersion>()
        }
    }

    override suspend fun getStatus(): Result<BusyBarStatus> {
        return runSafely {
            httpClient.get("/api/status").body<BusyBarStatus>()
        }
    }

    override suspend fun getStatusSystem(): Result<BusyBarStatusSystem> {
        return runSafely {
            httpClient.get("/api/status/system").body<BusyBarStatusSystem>()
        }
    }

    override suspend fun getStatusPower(): Result<BusyBarStatusPower> {
        return runSafely {
            httpClient.get("/api/status/power").body<BusyBarStatusPower>()
        }
    }
}
