package net.flipper.bsb.watchers.provisioning.fakes

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.generated.model.BusyBarStatus
import net.flipper.bridge.connection.feature.rpc.generated.model.BusyBarStatusDevice
import net.flipper.bridge.connection.feature.rpc.generated.model.BusyBarStatusPower
import net.flipper.bridge.connection.feature.rpc.generated.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusFirmware
import net.flipper.bridge.connection.feature.rpc.generated.model.VersionInfo

internal class FakeRpcSystemApi(
    private val deviceStatusResult: Result<BusyBarStatusDevice>
) : FRpcSystemApi {
    override suspend fun getVersion(): Result<VersionInfo> = error("Not used in test")
    override suspend fun getStatus(): Result<BusyBarStatus> = error("Not used in test")
    override suspend fun getDeviceStatus(): Result<BusyBarStatusDevice> = deviceStatusResult
    override suspend fun getStatusFirmware(): Result<StatusFirmware> = error("Not used in test")
    override suspend fun getStatusSystem(): Result<BusyBarStatusSystem> = error("Not used in test")
    override suspend fun getStatusPower(): Result<BusyBarStatusPower> = error("Not used in test")
}
