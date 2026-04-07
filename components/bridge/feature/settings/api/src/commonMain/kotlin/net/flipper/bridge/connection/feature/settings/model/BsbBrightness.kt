package net.flipper.bridge.connection.feature.settings.model

import net.flipper.core.busylib.data.Fraction

/**
 * Don't put @Serializable here
 *
 * @see net.flipper.bridge.connection.feature.rpc.api.serialization.BsbBrightnessSerializer
 * @see net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo
 */
sealed interface BsbBrightness {
    data object Auto : BsbBrightness

    /**
     * @param value as a normalized fraction
     * @see Fraction
     */
    data class Number(val value: Fraction) : BsbBrightness
}
