package net.flipper.bridge.connection.feature.rpc.api.exposed

interface FRpcBusyApi {
    suspend fun getBusySnapshot(): Result<String>
    suspend fun setBusySnapshot(rawJson: String): Result<Unit>
    suspend fun getBusyProfile(slot: String): Result<String>
    suspend fun setBusyProfile(slot: String, rawJson: String): Result<Unit>
}
