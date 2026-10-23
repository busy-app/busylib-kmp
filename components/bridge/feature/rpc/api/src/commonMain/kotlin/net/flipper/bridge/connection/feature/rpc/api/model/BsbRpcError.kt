package net.flipper.bridge.connection.feature.rpc.api.model

enum class BsbRpcError(val error: String) {
    ALREADY_CONNECTED("Already connected"),
    ALREADY_LINKED("Already linked"),
    NOT_CONNECTED("Not connected"),
    BATTERY_LOW("Battery level too low"),
    UPDATE_NOT_ALLOWED("Update not allowed: Battery level too low"),
    NOT_DRAWN_LOW_PRIORITY("Not drawn due to low priority"),
    NAME_DISALLOWED_CHARACTER("Name contains disallowed character"),
    NAME_EMPTY("Name is empty"),
    NAME_TOO_LONG("Name is too long")
}
