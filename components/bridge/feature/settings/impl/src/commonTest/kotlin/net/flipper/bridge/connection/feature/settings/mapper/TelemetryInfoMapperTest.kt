@file:Suppress("FunctionNaming")

package net.flipper.bridge.connection.feature.settings.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.TelemetryInfo
import net.flipper.bridge.connection.feature.settings.model.BsbTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryInfoMapperTest {
    @Test
    fun GIVEN_rpc_telemetry_WHEN_converted_THEN_consent_fields_are_preserved() {
        val telemetry = TelemetryInfo(
            enabled = true,
            asked = false
        )

        val result = telemetry.toBsbTelemetry()

        assertEquals(BsbTelemetry(enabled = true, asked = false), result)
    }

    @Test
    fun GIVEN_public_telemetry_WHEN_converted_THEN_consent_fields_are_preserved() {
        val telemetry = BsbTelemetry(
            enabled = false,
            asked = true
        )

        val result = telemetry.toTelemetryInfo()

        assertEquals(TelemetryInfo(enabled = false, asked = true), result)
    }
}
