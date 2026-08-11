package net.flipper.bridge.connection.feature.wifi.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

/**
 * Every case resolves the serializer from the sealed ancestor, never from the concrete subtype:
 * resolving from the subtype skips the branch where kotlinx would pick a polymorphic serializer,
 * and that branch is exactly where [WiFiSecurity.Supported.Password] used to blow up.
 */
class WiFiSecuritySerializationTest {
    private val json = Json
    private val securitySerializer: KSerializer<WiFiSecurity> = serializer()
    private val supportedSerializer: KSerializer<WiFiSecurity.Supported> = serializer()
    private val passwordSerializer: KSerializer<WiFiSecurity.Supported.Password> = serializer()

    private fun roundTripAsSecurity(security: WiFiSecurity): WiFiSecurity {
        return json.decodeFromString(
            securitySerializer,
            json.encodeToString(securitySerializer, security)
        )
    }

    private fun roundTripAsSupported(security: WiFiSecurity.Supported): WiFiSecurity.Supported {
        return json.decodeFromString(
            supportedSerializer,
            json.encodeToString(supportedSerializer, security)
        )
    }

    @Test
    fun GIVEN_every_password_security_WHEN_round_tripped_through_sealed_root_THEN_it_is_unchanged() {
        WiFiSecurity.Supported.Password.entries.forEach { password ->
            assertEquals(password, roundTripAsSecurity(password), password.name)
        }
    }

    @Test
    fun GIVEN_open_security_WHEN_round_tripped_through_sealed_root_THEN_it_is_unchanged() {
        assertEquals(WiFiSecurity.Supported.None, roundTripAsSecurity(WiFiSecurity.Supported.None))
    }

    @Test
    fun GIVEN_every_other_security_WHEN_round_tripped_through_sealed_root_THEN_it_is_unchanged() {
        BsbWifiSecurityMethod.entries.forEach { method ->
            val other = WiFiSecurity.Other(method)

            assertEquals(other, roundTripAsSecurity(other), method.name)
        }
    }

    @Test
    fun GIVEN_every_supported_security_WHEN_round_tripped_through_supported_ancestor_THEN_it_is_unchanged() {
        val supported = WiFiSecurity.Supported.Password.entries + WiFiSecurity.Supported.None

        supported.forEach { security ->
            assertEquals(security, roundTripAsSupported(security), security.toString())
        }
    }

    /**
     * [WiFiSecurity.Other] is not a [WiFiSecurity.Supported], so widening it back must not silently
     * resolve to a password protected network.
     */
    @Test
    fun GIVEN_other_security_payload_WHEN_decoded_as_supported_THEN_it_fails() {
        val encoded = json.encodeToString(
            securitySerializer,
            WiFiSecurity.Other(BsbWifiSecurityMethod.UNSUPPORTED)
        )

        assertFails {
            json.decodeFromString(supportedSerializer, encoded)
        }
    }

    /**
     * Pins the wire shape consumers already persisted: a field declared as the concrete
     * [WiFiSecurity.Supported.Password] type must keep encoding as a bare string.
     */
    @Test
    fun GIVEN_password_security_WHEN_encoded_by_concrete_type_THEN_it_stays_a_bare_string() {
        assertEquals(
            expected = "\"WPA2\"",
            actual = json.encodeToString(
                passwordSerializer,
                WiFiSecurity.Supported.Password.WPA2
            )
        )
    }
}
