package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatus
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusPower
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion

interface FRpcSystemApi {
    suspend fun getVersion(): Result<BusyBarVersion>

    suspend fun getStatus(): Result<BusyBarStatus>
    suspend fun getStatusSystem(): Result<BusyBarStatusSystem>
    suspend fun getStatusPower(): Result<BusyBarStatusPower>
}
