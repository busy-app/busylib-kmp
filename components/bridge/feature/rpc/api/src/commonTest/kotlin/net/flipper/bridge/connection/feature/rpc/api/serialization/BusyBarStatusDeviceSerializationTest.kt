package net.flipper.bridge.connection.feature.rpc.api.serialization

import kotlinx.serialization.json.Json
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class BusyBarStatusDeviceSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun GIVEN_status_device_json_with_otp_fields_WHEN_decoded_THEN_otp_model_and_seconds_timestamp_are_parsed() {
        val statusDevice = json.decodeFromString<BusyBarStatusDevice>(
            """
            {
                "serial_number": "BB123456",
                "usb_mac": "0c:fa:22:21:2a:31",
                "otp_valid": true,
                "otp_model": "BB.1",
                "otp_timestamp": 1767225600
            }
            """.trimIndent()
        )

        assertEquals(
            expected = "BB.1",
            actual = statusDevice.otpModel
        )
        assertEquals(
            expected = Instant.parse("2026-01-01T00:00:00Z"),
            actual = statusDevice.otpTimestamp
        )
    }

    @Test
    fun GIVEN_status_device_json_without_otp_fields_WHEN_decoded_THEN_otp_model_and_timestamp_are_null() {
        val statusDevice = json.decodeFromString<BusyBarStatusDevice>(
            """
            {
                "serial_number": "BB123456",
                "usb_mac": "0c:fa:22:21:2a:31",
                "otp_valid": false
            }
            """.trimIndent()
        )

        assertNull(statusDevice.otpModel)
        assertNull(statusDevice.otpTimestamp)
    }
}
