package net.flipper.bridge.connection.feature.telemetry.api.fake

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatus
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusDevice
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusPower
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.feature.rpc.api.model.StatusFirmware

internal class FakeRpcSystemApi(
    private val statusSystemResults: List<Result<BusyBarStatusSystem>>
) : FRpcSystemApi {
    var getStatusSystemCalls = 0
        private set

    override suspend fun getStatusSystem(): Result<BusyBarStatusSystem> {
        val result = statusSystemResults[minOf(getStatusSystemCalls, statusSystemResults.lastIndex)]
        getStatusSystemCalls++
        return result
    }

    override suspend fun getVersion(): Result<BusyBarVersion> = error("not used")

    override suspend fun getStatus(): Result<BusyBarStatus> = error("not used")

    override suspend fun getDeviceStatus(localOnly: Boolean): Result<BusyBarStatusDevice> =
        error("not used")

    override suspend fun getStatusFirmware(): Result<StatusFirmware> = error("not used")

    override suspend fun getStatusPower(): Result<BusyBarStatusPower> = error("not used")
}
