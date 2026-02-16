package net.flipper.bridge.connection.utils

import kotlin.uuid.Uuid

/**
 * Sample secrets used for demonstration purposes only.
 *
 * Replace these placeholder values with real credentials before running
 * against a real backend or authentication service.
 */
object Secrets {
    val DEVICE_ID = Uuid.parse("REPLACE_WITH_DEVICE_ID")
    const val AUTH_TOKEN = "REPLACE_WITH_AUTH_TOKEN"
}
