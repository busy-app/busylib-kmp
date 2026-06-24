package net.flipper.bridge.connection.feature.rpc.api.model

/**
 * Thrown when BUSY Bar replies to a critical RPC call with a structured error response,
 * for example `{"error":"Not connected"}`.
 *
 * It represents a known device-side error rather than a malformed/unparseable response,
 * so callers can handle it quietly (e.g. log a single line) instead of treating it as an
 * unexpected failure with a full stacktrace.
 */
class BsbRpcException(
    val error: String,
    cause: Throwable? = null,
) : Exception(error, cause)
