package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissioningPayload

interface FRpcMatterApi {
    suspend fun postMatterCommissioning(): Result<MatterCommissioningPayload>
    suspend fun deleteMatterCommissioning(): Result<Unit>
}
