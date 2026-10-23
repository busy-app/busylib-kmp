package net.flipper.bridge.connection.feature.settings.model

/**
 * The bar refused the name passed to `setDeviceName`, so it keeps its previous one.
 * Each case carries the bar's own message.
 */
sealed class DeviceNameError(message: String) : Exception(message) {
    class DisallowedCharacter : DeviceNameError("Name contains disallowed character")
    class Empty : DeviceNameError("Name is empty")
    class TooLong : DeviceNameError("Name is too long")
}
