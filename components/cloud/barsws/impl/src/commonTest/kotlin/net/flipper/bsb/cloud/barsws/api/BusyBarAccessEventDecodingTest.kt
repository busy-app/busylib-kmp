package net.flipper.bsb.cloud.barsws.api

import kotlinx.serialization.json.Json
import net.flipper.bsb.cloud.barsws.api.model.BusyBarAccessInternal
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal
import net.flipper.bsb.cloud.barsws.api.model.toPublic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BusyBarAccessEventDecodingTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(frame: String): WebSocketEventInternal {
        return json.decodeFromString(WebSocketEventInternal.serializer(), frame)
    }

    private fun accessFrame(access: String): String {
        return """{"type":"busy-bar-access.updated","busy_bar_access":$access}"""
    }

    @Test
    fun GIVEN_granted_frame_without_grace_WHEN_decoded_THEN_access_is_granted_without_grace() {
        val event = decode(accessFrame("""{"granted":true,"grace_until":null}"""))

        assertEquals(
            WebSocketEventInternal.BusyBarAccessUpdated(
                BusyBarAccessInternal(granted = true, graceUntil = null)
            ),
            event
        )
    }

    @Test
    fun GIVEN_frame_with_utc_grace_WHEN_decoded_THEN_grace_until_is_parsed() {
        val event = decode(accessFrame("""{"granted":true,"grace_until":"2026-10-22T10:00:00Z"}"""))

        assertEquals(
            WebSocketEventInternal.BusyBarAccessUpdated(
                BusyBarAccessInternal(granted = true, graceUntil = Instant.parse("2026-10-22T10:00:00Z"))
            ),
            event
        )
    }

    @Test
    fun GIVEN_frame_with_offset_and_fraction_grace_WHEN_decoded_THEN_grace_until_is_normalized_to_utc() {
        val event = decode(
            accessFrame("""{"granted":true,"grace_until":"2026-10-22T13:00:00.123456+03:00"}""")
        )

        assertEquals(
            WebSocketEventInternal.BusyBarAccessUpdated(
                BusyBarAccessInternal(
                    granted = true,
                    graceUntil = Instant.parse("2026-10-22T10:00:00.123456Z")
                )
            ),
            event
        )
    }

    @Test
    fun GIVEN_revoked_frame_without_grace_field_WHEN_decoded_THEN_access_is_revoked() {
        val event = decode(
            """{"type":"busy-bar-access.updated","busy_bar_access":{"granted":false},"user_id":"u1"}"""
        )

        assertEquals(
            WebSocketEventInternal.BusyBarAccessUpdated(
                BusyBarAccessInternal(granted = false, graceUntil = null)
            ),
            event
        )
    }

    @Test
    fun GIVEN_access_event_WHEN_mapped_to_public_THEN_fields_are_kept() {
        val graceUntil = Instant.parse("2026-10-22T10:00:00Z")
        val event = WebSocketEventInternal.BusyBarAccessUpdated(
            BusyBarAccessInternal(granted = true, graceUntil = graceUntil)
        )

        assertEquals(
            WebSocketEvent.BusyBarAccessUpdatedEvent(granted = true, graceUntil = graceUntil),
            event.toPublic()
        )
    }
}
