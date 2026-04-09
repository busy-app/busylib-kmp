package net.flipper.bridge.connection.service.model

enum class ForgetDeviceResult {
    COULD_NOT_UNLINK_CLOUD_ACCOUNT,
    SUCCESS,
    DEVICE_NOT_FOUND,
    NOT_AUTHORIZED,
    COULD_NOT_GET_CLOUD_BARS_LIST
}
