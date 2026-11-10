package net.flipper.bridge.connection.feature.telemetry.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.telemetry.model.BsbTelemetry
import net.flipper.busylib.core.wrapper.CResult

interface FTelemetryFeatureApi : FDeviceFeatureApi {
    suspend fun getTelemetry(): CResult<BsbTelemetry>
    suspend fun setTelemetry(telemetry: BsbTelemetry): CResult<Unit>
}
