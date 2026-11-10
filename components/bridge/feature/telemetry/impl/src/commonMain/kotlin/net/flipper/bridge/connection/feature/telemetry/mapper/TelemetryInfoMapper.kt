package net.flipper.bridge.connection.feature.telemetry.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.TelemetryInfo
import net.flipper.bridge.connection.feature.telemetry.model.BsbTelemetry

internal fun TelemetryInfo.toBsbTelemetry(): BsbTelemetry {
    return BsbTelemetry(
        enabled = enabled,
        asked = asked
    )
}

internal fun BsbTelemetry.toTelemetryInfo(): TelemetryInfo {
    return TelemetryInfo(
        enabled = enabled,
        asked = asked
    )
}
