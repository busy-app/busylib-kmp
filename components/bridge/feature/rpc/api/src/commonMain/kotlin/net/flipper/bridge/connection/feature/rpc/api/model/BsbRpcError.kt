package net.flipper.bridge.connection.feature.rpc.api.model

enum class BsbRpcError(val error: String) {
    ALREADY_CONNECTED("Already connected"),
    ALREADY_LINKED("Already linked")
}
