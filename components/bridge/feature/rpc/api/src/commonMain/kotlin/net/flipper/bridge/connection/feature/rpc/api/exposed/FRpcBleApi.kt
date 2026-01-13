package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.BleStatusResponse

interface FRpcBleApi {
    suspend fun getBleStatus(ignoreCache: Boolean): Result<BleStatusResponse>
}
