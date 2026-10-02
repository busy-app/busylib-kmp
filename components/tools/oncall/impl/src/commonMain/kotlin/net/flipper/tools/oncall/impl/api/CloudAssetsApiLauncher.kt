package net.flipper.tools.oncall.impl.api

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import kotlin.uuid.Uuid

fun interface CloudAssetsApiLauncher {
    suspend fun withAssetsApi(
        deviceId: Uuid,
        block: suspend (FRpcAssetsApi) -> Unit
    ): Result<Unit>
}
