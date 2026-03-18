package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlin.test.Test
import kotlin.test.assertEquals

class BluetoothUuidNormalizerTest {

    @Test
    fun GIVEN_full_uuid_with_dashes_WHEN_normalizing_THEN_return_lowercase_full_uuid() {
        val rawUuid = "6E400004-B5A3-F393-E0A9-E50E24DCCA9E"

        val normalized = normalizeBluetoothUuid(rawUuid)

        assertEquals("6e400004-b5a3-f393-e0a9-e50e24dcca9e", normalized)
    }

    @Test
    fun GIVEN_full_uuid_without_dashes_WHEN_normalizing_THEN_insert_dashes() {
        val rawUuid = "6E400004B5A3F393E0A9E50E24DCCA9E"

        val normalized = normalizeBluetoothUuid(rawUuid)

        assertEquals("6e400004-b5a3-f393-e0a9-e50e24dcca9e", normalized)
    }

    @Test
    fun GIVEN_16_bit_short_uuid_WHEN_normalizing_THEN_expand_to_bluetooth_base_uuid() {
        val normalized = normalizeBluetoothUuid("308A")

        assertEquals("0000308a-0000-1000-8000-00805f9b34fb", normalized)
    }

    @Test
    fun GIVEN_32_bit_short_uuid_WHEN_normalizing_THEN_use_current_heuristic_behavior() {
        val normalized = normalizeBluetoothUuid("12345678")

        assertEquals("000012345678-0000-1000-8000-00805f9b34fb", normalized)
    }

    @Test
    fun GIVEN_unexpected_format_with_hex_digits_WHEN_normalizing_THEN_fallback_to_first_4_hex_digits() {
        val normalized = normalizeBluetoothUuid("zz18-0A--foo")

        assertEquals("0000180a-0000-1000-8000-00805f9b34fb", normalized)
    }

    @Test
    fun GIVEN_unexpected_format_without_enough_hex_digits_WHEN_normalizing_THEN_return_lowercase_as_is() {
        val normalized = normalizeBluetoothUuid("ZZG")

        assertEquals("zzg", normalized)
    }
}
