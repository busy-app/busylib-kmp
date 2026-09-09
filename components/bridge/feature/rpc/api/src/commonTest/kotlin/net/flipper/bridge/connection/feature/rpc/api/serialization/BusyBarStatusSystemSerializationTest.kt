package net.flipper.bridge.connection.feature.rpc.api.serialization

import kotlinx.serialization.json.Json
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BusyBarStatusSystemSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun GIVEN_status_system_json_WHEN_decoded_THEN_zero_padded_uptime_is_parsed() {
        val statusSystem = json.decodeFromString<BusyBarStatusSystem>(
            """
            {
                "api_semver": "6.3.0",
                "uptime": "00d 00h 00m 46s",
                "boot_time": 0
            }
            """.trimIndent()
        )

        assertEquals(
            expected = 46.seconds,
            actual = statusSystem.uptime
        )
    }

    @Test
    fun GIVEN_status_system_json_WHEN_decoded_THEN_every_uptime_component_is_parsed() {
        val statusSystem = json.decodeFromString<BusyBarStatusSystem>(
            """
            {
                "api_semver": "6.3.0",
                "uptime": "00d 04h 48m 56s",
                "boot_time": 0
            }
            """.trimIndent()
        )

        assertEquals(
            expected = 4.hours + 48.minutes + 56.seconds,
            actual = statusSystem.uptime
        )
    }
}
