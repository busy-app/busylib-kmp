package net.flipper.bridge.connection.orchestrator.api.model

enum class DisconnectStatus {
    NOT_INITIALIZED,
    REPORTED_BY_TRANSPORT,
    ERROR_UNKNOWN,
    REQUIRES_REPAIRING;

    val isRecoverable: Boolean
        get() = when (this) {
            NOT_INITIALIZED,
            REPORTED_BY_TRANSPORT,
            ERROR_UNKNOWN -> true
            REQUIRES_REPAIRING -> false
        }
}
