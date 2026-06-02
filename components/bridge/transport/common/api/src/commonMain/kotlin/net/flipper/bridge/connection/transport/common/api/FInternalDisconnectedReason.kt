package net.flipper.bridge.connection.transport.common.api

enum class FInternalDisconnectedReason {
    REQUIRES_REPAIRING,
    OTHER;

    val isRecoverable: Boolean
        get() = when (this) {
            REQUIRES_REPAIRING -> false
            OTHER -> true
        }
}
