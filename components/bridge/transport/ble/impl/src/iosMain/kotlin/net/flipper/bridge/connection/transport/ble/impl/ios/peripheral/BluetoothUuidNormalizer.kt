@file:Suppress("MagicNumber")

package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.warn

private const val UUID_WITH_DASHES_LENGTH = 36
private const val UUID_WITHOUT_DASHES_LENGTH = 32
private const val SHORT_UUID_LENGTH = 4
private const val LONG_SHORT_UUID_LENGTH = 8
private const val BLUETOOTH_BASE_UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb"

/**
 * Convert short-form Bluetooth SIG UUIDs to full 128-bit format.
 * Standard Bluetooth UUIDs use the base: 0000XXXX-0000-1000-8000-00805f9b34fb
 * where XXXX is the short form (16-bit or 32-bit).
 */
internal fun normalizeBluetoothUuid(uuidString: String): String {
    val lowercase = uuidString.lowercase()

    // If it already has dashes and is 36 characters, it's already a full UUID.
    if (lowercase.contains("-") && lowercase.length == UUID_WITH_DASHES_LENGTH) {
        return lowercase
    }

    // Remove dashes for processing.
    val withoutDashes = lowercase.replace("-", "")

    // Already a full UUID without dashes (32 hex chars).
    if (withoutDashes.length == UUID_WITHOUT_DASHES_LENGTH) {
        return "${withoutDashes.substring(0, 8)}-${withoutDashes.substring(8, 12)}-" +
            "${withoutDashes.substring(12, 16)}-${withoutDashes.substring(16, 20)}-" +
            withoutDashes.substring(20, 32)
    }

    // Short form UUID (4 or 8 characters) - convert to full Bluetooth SIG UUID.
    if (withoutDashes.length == SHORT_UUID_LENGTH || withoutDashes.length == LONG_SHORT_UUID_LENGTH) {
        val paddedUuid = withoutDashes.padStart(SHORT_UUID_LENGTH, '0')
        return "0000$paddedUuid$BLUETOOTH_BASE_UUID_SUFFIX"
    }

    // Unknown format - try to parse as hex and convert to Bluetooth SIG format.
    // This handles malformed UUIDs by extracting just the service ID part.
    warn { "Unexpected UUID format: $uuidString (length=${withoutDashes.length}) " }

    val hexDigits = withoutDashes.filter { it in "0123456789abcdef" }
    val serviceId = when {
        hexDigits.length >= SHORT_UUID_LENGTH -> hexDigits.substring(0, SHORT_UUID_LENGTH)
            .padStart(SHORT_UUID_LENGTH, '0')
        else -> {
            error { "Unable to normalize UUID: $uuidString" }
            return lowercase
        }
    }

    return "0000$serviceId$BLUETOOTH_BASE_UUID_SUFFIX"
}
