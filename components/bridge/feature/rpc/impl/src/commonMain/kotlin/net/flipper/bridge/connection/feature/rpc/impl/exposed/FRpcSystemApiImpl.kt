package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.generated.api.SystemApi
import net.flipper.bridge.connection.feature.rpc.generated.model.NetworkInterfaceInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.Status
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusDevice
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusFirmware
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusPower
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusSystem
import net.flipper.bridge.connection.feature.rpc.generated.model.VersionInfo
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcSystemApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : SystemApi {
    override suspend fun getVersion(): Result<VersionInfo> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/version").body<VersionInfo>()
        }
    }

    override suspend fun getStatus(): Result<Status> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/status").body<Status>()
        }
    }

    override suspend fun getStatusDevice(): Result<StatusDevice> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/status/device").body<StatusDevice>()
        }
    }

    override suspend fun getStatusFirmware(): Result<StatusFirmware> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/status/firmware").body<StatusFirmware>()
        }
    }

    override suspend fun getStatusSystem(): Result<StatusSystem> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/status/system").body<StatusSystem>()
        }
    }

    override suspend fun getTransport(): Result<NetworkInterfaceInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getStatusPower(): Result<StatusPower> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/status/power").body<StatusPower>()
        }
    }
}
