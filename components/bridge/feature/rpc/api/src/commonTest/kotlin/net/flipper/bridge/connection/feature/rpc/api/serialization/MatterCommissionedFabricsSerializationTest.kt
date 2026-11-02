package net.flipper.bridge.connection.feature.rpc.api.serialization

import kotlinx.serialization.json.Json
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class MatterCommissionedFabricsSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun GIVEN_pairing_json_WHEN_decoded_THEN_timestamp_is_read_as_epoch_seconds() {
        val fabrics = json.decodeFromString<MatterCommissionedFabrics>(
            """
            {
                "fabric_count": 2,
                "latest_pairing_status": {
                    "value": "completed_successfully",
                    "timestamp": 1793636519
                }
            }
            """.trimIndent()
        )

        assertEquals(
            expected = Instant.fromEpochSeconds(1_793_636_519),
            actual = fabrics.latestCommissioningStatus.timestamp
        )
    }

    @Test
    fun GIVEN_pairing_json_without_timestamp_WHEN_decoded_THEN_timestamp_is_null() {
        val fabrics = json.decodeFromString<MatterCommissionedFabrics>(
            """
            {
                "fabric_count": 2,
                "latest_pairing_status": {
                    "value": "never_started"
                }
            }
            """.trimIndent()
        )

        assertNull(fabrics.latestCommissioningStatus.timestamp)
    }
}
