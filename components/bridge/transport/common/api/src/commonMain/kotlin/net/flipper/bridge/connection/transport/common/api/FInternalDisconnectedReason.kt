package net.flipper.bridge.connection.transport.common.api

enum class FInternalDisconnectedReason {
    REQUIRES_REPAIRING,
    REQUIRES_PERMISSION,
    OTHER;

    val isRecoverable: Boolean
        get() = when (this) {
            REQUIRES_REPAIRING,
            REQUIRES_PERMISSION -> false

            OTHER -> true
        }
}
